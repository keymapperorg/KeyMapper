package io.github.sds100.keymapper.mappings.keymaps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.ActionErrorSnapshot
import io.github.sds100.keymapper.backup.BackupRestoreMappingsUseCase
import io.github.sds100.keymapper.backup.ImportExportState
import io.github.sds100.keymapper.backup.RestoreType
import io.github.sds100.keymapper.constraints.ConstraintErrorSnapshot
import io.github.sds100.keymapper.constraints.ConstraintMode
import io.github.sds100.keymapper.constraints.ConstraintUiHelper
import io.github.sds100.keymapper.groups.SubGroupListModel
import io.github.sds100.keymapper.home.HomeWarningListItem
import io.github.sds100.keymapper.home.SelectedKeyMapsEnabled
import io.github.sds100.keymapper.home.ShowHomeScreenAlertsUseCase
import io.github.sds100.keymapper.mappings.PauseKeyMapsUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.mappings.keymaps.trigger.SetupGuiKeyboardState
import io.github.sds100.keymapper.mappings.keymaps.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerError
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerErrorSnapshot
import io.github.sds100.keymapper.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.sorting.SortViewModel
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.onSuccess
import io.github.sds100.keymapper.util.ui.DialogResponse
import io.github.sds100.keymapper.util.ui.MultiSelectProvider
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigateEvent
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SelectionState
import io.github.sds100.keymapper.util.ui.ViewModelHelper
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.util.ui.navigate
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeyMapListViewModel(
    private val coroutineScope: CoroutineScope,
    private val listKeyMaps: ListKeyMapsUseCase,
    resourceProvider: ResourceProvider,
    private val setupGuiKeyboard: SetupGuiKeyboardUseCase,
    private val sortKeyMaps: SortKeyMapsUseCase,
    private val showAlertsUseCase: ShowHomeScreenAlertsUseCase,
    private val pauseKeyMaps: PauseKeyMapsUseCase,
    private val backupRestore: BackupRestoreMappingsUseCase,

) : PopupViewModel by PopupViewModelImpl(),
    ResourceProvider by resourceProvider,
    NavigationViewModel by NavigationViewModelImpl() {

    private companion object {
        const val ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM = "accessibility_service_disabled"
        const val ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM = "accessibility_service_crashed"
        const val ID_BATTERY_OPTIMISATION_LIST_ITEM = "battery_optimised"
        const val ID_LOGGING_ENABLED_LIST_ITEM = "logging_enabled"
    }

    val sortViewModel = SortViewModel(coroutineScope, sortKeyMaps)
    var showSortBottomSheet by mutableStateOf(false)

    val multiSelectProvider: MultiSelectProvider = MultiSelectProvider()

    private val listItemCreator = KeyMapListItemCreator(listKeyMaps, resourceProvider)
    private val constraintUiHelper = ConstraintUiHelper(listKeyMaps, resourceProvider)

    private val initialState = KeyMapListState(
        appBarState = KeyMapAppBarState.RootGroup(
            subGroups = emptyList(),
            warnings = emptyList(),
            isPaused = false,
        ),
        listItems = State.Loading,
    )
    private val _state: MutableStateFlow<KeyMapListState> = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    var showFabText: Boolean by mutableStateOf(true)

    val setupGuiKeyboardState: StateFlow<SetupGuiKeyboardState> = combine(
        setupGuiKeyboard.isInstalled,
        setupGuiKeyboard.isEnabled,
        setupGuiKeyboard.isChosen,
    ) { isInstalled, isEnabled, isChosen ->
        SetupGuiKeyboardState(
            isInstalled,
            isEnabled,
            isChosen,
        )
    }.stateIn(coroutineScope, SharingStarted.Lazily, SetupGuiKeyboardState.DEFAULT)

    var showDpadTriggerSetupBottomSheet: Boolean by mutableStateOf(false)

    private val keyMapGroupStateFlow = listKeyMaps.keyMapGroup.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        KeyMapGroup(
            group = null,
            subGroups = emptyList(),
            keyMaps = State.Loading,
            parents = emptyList(),
        ),
    )

    private val _importExportState = MutableStateFlow<ImportExportState>(ImportExportState.Idle)
    val importExportState: StateFlow<ImportExportState> = _importExportState.asStateFlow()

    private val warnings: Flow<List<HomeWarningListItem>> = combine(
        showAlertsUseCase.isBatteryOptimised,
        showAlertsUseCase.accessibilityServiceState,
        showAlertsUseCase.hideAlerts,
        showAlertsUseCase.isLoggingEnabled,
    ) { isBatteryOptimised, serviceState, isHidden, isLoggingEnabled ->
        if (isHidden) {
            return@combine emptyList()
        }

        buildList {
            when (serviceState) {
                ServiceState.CRASHED ->
                    add(
                        HomeWarningListItem(
                            ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM,
                            getString(R.string.home_error_accessibility_service_is_crashed),
                        ),
                    )

                ServiceState.DISABLED ->
                    add(
                        HomeWarningListItem(
                            ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM,
                            getString(R.string.home_error_accessibility_service_is_disabled),
                        ),
                    )

                ServiceState.ENABLED -> {}
            }

            if (isBatteryOptimised) {
                add(
                    HomeWarningListItem(
                        ID_BATTERY_OPTIMISATION_LIST_ITEM,
                        getString(R.string.home_error_is_battery_optimised),
                    ),
                )
            } // don't show a success message for this

            if (isLoggingEnabled) {
                add(
                    HomeWarningListItem(
                        ID_LOGGING_ENABLED_LIST_ITEM,
                        getString(R.string.home_error_logging_enabled),
                    ),
                )
            }
        }
    }

    /**
     * Whether the current group was just created and hasn't been saved with a user defined
     * name yet.
     */
    private var isNewGroup = false
    var isEditingGroupName by mutableStateOf(false)

    init {
        val sortedKeyMapsFlow = combine(
            keyMapGroupStateFlow.map { it.keyMaps }.distinctUntilChanged(),
            sortKeyMaps.observeKeyMapsSorter(),
        ) { keyMapsState, sorter ->
            keyMapsState.mapData { list -> list.sortedWith(sorter) }
        }.flowOn(Dispatchers.Default)

        val listItemContentFlow =
            combine(
                sortedKeyMapsFlow,
                listKeyMaps.showDeviceDescriptors,
                listKeyMaps.triggerErrorSnapshot,
                listKeyMaps.actionErrorSnapshot,
                listKeyMaps.constraintErrorSnapshot,
                transform = ::buildListItems,
            ).flowOn(Dispatchers.Default)

        // The list item content should be separate from the selection state
        // because creating the content is an expensive operation and selection should be almost
        // instantaneous.
        val listItemStateFlow = combine(
            listItemContentFlow,
            multiSelectProvider.state,
        ) { contentListState, selectionState ->
            contentListState.mapData { contentList ->
                if (selectionState is SelectionState.Selecting) {
                    contentList.map { item ->
                        KeyMapListItemModel(
                            isSelected = selectionState.selectedIds.contains(item.uid),
                            content = item,
                        )
                    }
                } else {
                    contentList.map { contentListItem ->
                        KeyMapListItemModel(isSelected = false, contentListItem)
                    }
                }
            }
        }

        val appBarStateFlow = combine(
            keyMapGroupStateFlow,
            warnings,
            multiSelectProvider.state,
            pauseKeyMaps.isPaused,
            listKeyMaps.constraintErrorSnapshot,
            transform = ::buildAppBarState,
        )

        coroutineScope.launch {
            combine(
                listItemStateFlow,
                appBarStateFlow,
            ) { listState, appBarState ->
                Pair(listState, appBarState)
            }.collectLatest { (listState, appBarState) ->
                listState.ifIsData { list ->
                    if (list.isNotEmpty()) {
                        showFabText = false
                    }
                }

                _state.value = KeyMapListState(appBarState, listState)
            }
        }

        coroutineScope.launch {
            backupRestore.onAutomaticBackupResult.collectLatest { result ->
                onAutomaticBackupResult(result)
            }
        }
    }

    private fun buildAppBarState(
        keyMapGroup: KeyMapGroup,
        warnings: List<HomeWarningListItem>,
        selectionState: SelectionState,
        isPaused: Boolean,
        constraintErrorSnapshot: ConstraintErrorSnapshot,
    ): KeyMapAppBarState {
        if (selectionState is SelectionState.Selecting) {
            var selectedKeyMapsEnabled: SelectedKeyMapsEnabled? = null
            val keyMaps = keyMapGroup.keyMaps.dataOrNull() ?: emptyList()

            for (keyMap in keyMaps) {
                if (keyMap.uid in selectionState.selectedIds) {
                    if (selectedKeyMapsEnabled == null) {
                        selectedKeyMapsEnabled = if (keyMap.isEnabled) {
                            SelectedKeyMapsEnabled.ALL
                        } else {
                            SelectedKeyMapsEnabled.NONE
                        }
                    } else {
                        if ((keyMap.isEnabled && selectedKeyMapsEnabled == SelectedKeyMapsEnabled.NONE) ||
                            (!keyMap.isEnabled && selectedKeyMapsEnabled == SelectedKeyMapsEnabled.ALL)
                        ) {
                            selectedKeyMapsEnabled = SelectedKeyMapsEnabled.MIXED
                            break
                        }
                    }
                }
            }

            return KeyMapAppBarState.Selecting(
                selectionCount = selectionState.selectedIds.size,
                selectedKeyMapsEnabled = selectedKeyMapsEnabled ?: SelectedKeyMapsEnabled.NONE,
                isAllSelected = selectionState.selectedIds.size == keyMaps.size,
            )
        } else {
            val subGroupListItems = keyMapGroup.subGroups.map { group ->
                var icon: ComposeIconInfo? = null

                val constraint = group.constraintState.constraints.firstOrNull()
                if (constraint != null) {
                    icon = constraintUiHelper.getIcon(constraint)
                }

                SubGroupListModel(
                    uid = group.uid,
                    name = group.name,
                    icon = icon,
                )
            }

            val parentGroupListItems = keyMapGroup.parents.map { group ->
                SubGroupListModel(
                    uid = group.uid,
                    name = group.name,
                    icon = null,
                )
            }

            if (keyMapGroup.group == null) {
                return KeyMapAppBarState.RootGroup(
                    subGroups = subGroupListItems,
                    warnings = warnings,
                    isPaused = isPaused,
                )
            } else {
                return KeyMapAppBarState.ChildGroup(
                    groupName = keyMapGroup.group.name,
                    constraints = listItemCreator.buildConstraintChipList(
                        keyMapGroup.group.constraintState,
                        constraintErrorSnapshot,
                    ),
                    constraintMode = keyMapGroup.group.constraintState.mode,
                    subGroups = subGroupListItems,
                    parentGroups = parentGroupListItems,
                )
            }
        }
    }

    private fun buildListItems(
        keyMapsState: State<List<KeyMap>>,
        showDeviceDescriptors: Boolean,
        triggerErrorSnapshot: TriggerErrorSnapshot,
        actionErrorSnapshot: ActionErrorSnapshot,
        constraintErrorSnapshot: ConstraintErrorSnapshot,
    ): State<List<KeyMapListItemModel.Content>> {
        return keyMapsState.mapData { list ->
            list.map {
                listItemCreator.build(
                    it,
                    showDeviceDescriptors,
                    triggerErrorSnapshot,
                    actionErrorSnapshot,
                    constraintErrorSnapshot,
                )
            }
        }
    }

    fun onKeyMapCardClick(uid: String) {
        if (multiSelectProvider.state.value is SelectionState.Selecting) {
            multiSelectProvider.toggleSelection(uid)

            if (multiSelectProvider.getSelectedIds().isEmpty()) {
                multiSelectProvider.stopSelecting()
            }
        } else {
            coroutineScope.launch {
                navigate("config_key_map", NavDestination.ConfigKeyMap.Open(uid))
            }
        }
    }

    fun onKeyMapCardLongClick(uid: String) {
        if (multiSelectProvider.state.value is SelectionState.NotSelecting) {
            multiSelectProvider.startSelecting()
            multiSelectProvider.select(uid)
        }
    }

    fun onKeyMapSelectedChanged(uid: String, isSelected: Boolean) {
        if (isSelected) {
            multiSelectProvider.select(uid)
        } else {
            multiSelectProvider.deselect(uid)

            if (multiSelectProvider.getSelectedIds().isEmpty()) {
                multiSelectProvider.stopSelecting()
            }
        }
    }

    fun onFixTriggerError(error: TriggerError) {
        coroutineScope.launch {
            when (error) {
                TriggerError.DND_ACCESS_DENIED -> {
                    ViewModelHelper.showDialogExplainingDndAccessBeingUnavailable(
                        resourceProvider = this@KeyMapListViewModel,
                        popupViewModel = this@KeyMapListViewModel,
                        neverShowDndTriggerErrorAgain = { listKeyMaps.neverShowDndTriggerError() },
                        fixError = { listKeyMaps.fixTriggerError(error) },
                    )
                }

                TriggerError.DPAD_IME_NOT_SELECTED -> {
                    showDpadTriggerSetupBottomSheet = true
                }

                TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED, TriggerError.FLOATING_BUTTONS_NOT_PURCHASED -> {
                    navigate(
                        "purchase_advanced_trigger",
                        NavDestination.ConfigKeyMap.New(
                            groupUid = null,
                            showAdvancedTriggers = true,
                        ),
                    )
                }

                else -> {
                    listKeyMaps.fixTriggerError(error)
                }
            }
        }
    }

    fun onFixClick(error: Error) {
        coroutineScope.launch {
            when (error) {
                Error.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY) -> {
                    ViewModelHelper.showDialogExplainingDndAccessBeingUnavailable(
                        resourceProvider = this@KeyMapListViewModel,
                        popupViewModel = this@KeyMapListViewModel,
                        neverShowDndTriggerErrorAgain = { listKeyMaps.neverShowDndTriggerError() },
                        fixError = { listKeyMaps.fixError(error) },
                    )
                }

                else -> {
                    ViewModelHelper.showFixErrorDialog(
                        resourceProvider = this@KeyMapListViewModel,
                        popupViewModel = this@KeyMapListViewModel,
                        error,
                    ) {
                        listKeyMaps.fixError(error)
                    }
                }
            }
        }
    }

    fun onEnableGuiKeyboardClick() {
        setupGuiKeyboard.enableInputMethod()
    }

    fun onChooseGuiKeyboardClick() {
        setupGuiKeyboard.chooseInputMethod()
    }

    fun onNeverShowSetupDpadClick() {
        listKeyMaps.neverShowDpadImeSetupError()
    }

    fun onSelectAllClick() {
        state.value.also { state ->
            if (state.appBarState is KeyMapAppBarState.Selecting) {
                if (state.appBarState.isAllSelected) {
                    multiSelectProvider.stopSelecting()
                } else {
                    state.listItems.apply {
                        if (this is State.Data) {
                            multiSelectProvider.select(
                                *this.data.map { it.uid }.toTypedArray(),
                            )
                        }
                    }
                }
            }
        }
    }

    fun onEnabledKeyMapsChange(enabled: Boolean) {
        val selectionState = multiSelectProvider.state.value

        if (selectionState !is SelectionState.Selecting) return
        val selectedIds = selectionState.selectedIds

        if (enabled) {
            listKeyMaps.enableKeyMap(*selectedIds.toTypedArray())
        } else {
            listKeyMaps.disableKeyMap(*selectedIds.toTypedArray())
        }
    }

    fun onDuplicateSelectedKeyMapsClick() {
        val selectionState = multiSelectProvider.state.value

        if (selectionState !is SelectionState.Selecting) return
        val selectedIds = selectionState.selectedIds

        listKeyMaps.duplicateKeyMap(*selectedIds.toTypedArray())
    }

    fun onDeleteSelectedKeyMapsClick() {
        val selectionState = multiSelectProvider.state.value

        if (selectionState !is SelectionState.Selecting) return
        val selectedIds = selectionState.selectedIds.toTypedArray()

        listKeyMaps.deleteKeyMap(*selectedIds)
        multiSelectProvider.deselect(*selectedIds)
        multiSelectProvider.stopSelecting()
    }

    fun onExportSelectedKeyMaps() {
        val selectionState = multiSelectProvider.state.value

        if (selectionState !is SelectionState.Selecting) return

        coroutineScope.launch {
            val selectedIds = selectionState.selectedIds

            listKeyMaps.backupKeyMaps(*selectedIds.toTypedArray()).onSuccess {
                _importExportState.value = ImportExportState.FinishedExport(it)
            }.onFailure {
                _importExportState.value =
                    ImportExportState.Error(it.getFullMessage(this@KeyMapListViewModel))
            }
        }
    }

    fun onFixWarningClick(id: String) {
        coroutineScope.launch {
            when (id) {
                ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM -> {
                    val explanationResponse =
                        ViewModelHelper.showAccessibilityServiceExplanationDialog(
                            resourceProvider = this@KeyMapListViewModel,
                            popupViewModel = this@KeyMapListViewModel,
                        )

                    if (explanationResponse != DialogResponse.POSITIVE) {
                        return@launch
                    }

                    if (!showAlertsUseCase.startAccessibilityService()) {
                        ViewModelHelper.handleCantFindAccessibilitySettings(
                            resourceProvider = this@KeyMapListViewModel,
                            popupViewModel = this@KeyMapListViewModel,
                        )
                    }
                }

                ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM ->
                    ViewModelHelper.handleKeyMapperCrashedDialog(
                        resourceProvider = this@KeyMapListViewModel,
                        popupViewModel = this@KeyMapListViewModel,
                        restartService = showAlertsUseCase::restartAccessibilityService,
                        ignoreCrashed = showAlertsUseCase::acknowledgeCrashed,
                    )

                ID_BATTERY_OPTIMISATION_LIST_ITEM -> showAlertsUseCase.disableBatteryOptimisation()
                ID_LOGGING_ENABLED_LIST_ITEM -> showAlertsUseCase.disableLogging()
            }
        }
    }

    fun onTogglePausedClick() {
        coroutineScope.launch {
            if (pauseKeyMaps.isPaused.first()) {
                pauseKeyMaps.resume()
            } else {
                pauseKeyMaps.pause()
            }
        }
    }

    fun onExportClick() {
        coroutineScope.launch {
            if (_importExportState.value != ImportExportState.Idle) {
                return@launch
            }

            _importExportState.value = ImportExportState.Exporting
            backupRestore.backupEverything().onSuccess {
                _importExportState.value = ImportExportState.FinishedExport(it)
            }.onFailure {
                _importExportState.value =
                    ImportExportState.Error(it.getFullMessage(this@KeyMapListViewModel))
            }
        }
    }

    fun onChooseImportFile(uri: String) {
        coroutineScope.launch {
            backupRestore.getKeyMapCountInBackup(uri).onSuccess {
                _importExportState.value = ImportExportState.ConfirmImport(uri, it)
            }.onFailure {
                _importExportState.value =
                    ImportExportState.Error(it.getFullMessage(this@KeyMapListViewModel))
            }
        }
    }

    fun onConfirmImport(restoreType: RestoreType) {
        val state = _importExportState.value as? ImportExportState.ConfirmImport
        state ?: return

        _importExportState.value = ImportExportState.Importing

        coroutineScope.launch {
            backupRestore.restoreKeyMaps(state.fileUri, restoreType).onSuccess {
                _importExportState.value = ImportExportState.FinishedImport
            }.onFailure {
                _importExportState.value =
                    ImportExportState.Error(it.getFullMessage(this@KeyMapListViewModel))
            }
        }
    }

    fun setImportExportIdle() {
        _importExportState.value = ImportExportState.Idle
    }

    fun onBackClick(): Boolean {
        when {
            multiSelectProvider.state.value is SelectionState.Selecting -> {
                multiSelectProvider.stopSelecting()
                return true
            }

            state.value.appBarState is KeyMapAppBarState.ChildGroup -> {
                if (isEditingGroupName) {
                    if (isNewGroup) {
                        coroutineScope.launch {
                            listKeyMaps.deleteGroup()
                        }
                    } else {
                        isEditingGroupName = false
                    }
                } else {
                    coroutineScope.launch {
                        listKeyMaps.popGroup()
                    }
                }

                isEditingGroupName = false
                return true
            }

            else -> {
                return false
            }
        }
    }

    suspend fun onRenameGroupClick(name: String): Boolean {
        return listKeyMaps.renameGroup(name).also { success ->
            if (success) {
                isEditingGroupName = false
            }
        }
    }

    fun onEditGroupNameClick() {
        isEditingGroupName = true
    }

    fun onGroupClick(uid: String?) {
        coroutineScope.launch {
            listKeyMaps.openGroup(uid)
        }
    }

    fun onDeleteGroupClick() {
        coroutineScope.launch {
            listKeyMaps.deleteGroup()
        }
    }

    fun onNewGroupClick() {
        coroutineScope.launch {
            listKeyMaps.newGroup()
            isNewGroup = true
            isEditingGroupName = true
        }
    }

    fun onNewGroupConstraintClick() {
        coroutineScope.launch {
            val constraint = navigate(
                "add_group_constraint",
                NavDestination.ChooseConstraint,
            ) ?: return@launch

            listKeyMaps.addGroupConstraint(constraint)
        }
    }

    fun onRemoveGroupConstraintClick(uid: String) {
        coroutineScope.launch {
            listKeyMaps.removeGroupConstraint(uid)
        }
    }

    fun onGroupConstraintModeChanged(mode: ConstraintMode) {
        coroutineScope.launch {
            listKeyMaps.setGroupConstraintMode(mode)
        }
    }

    fun onNewKeyMapClick() {
        coroutineScope.launch {
            val groupUid = listKeyMaps.keyMapGroup.first().group?.uid

            navigate(
                NavigateEvent(
                    "config_key_map",
                    NavDestination.ConfigKeyMap.New(groupUid = groupUid),
                ),
            )
        }
    }

    private suspend fun onAutomaticBackupResult(result: Result<*>) {
        when (result) {
            is Success -> {}

            is Error -> {
                val response = showPopup(
                    "automatic_backup_error",
                    PopupUi.Dialog(
                        title = getString(R.string.toast_automatic_backup_failed),
                        message = result.getFullMessage(this),
                        positiveButtonText = getString(R.string.pos_ok),
                        neutralButtonText = getString(R.string.neutral_go_to_settings),
                    ),
                ) ?: return

                if (response == DialogResponse.NEUTRAL) {
                    navigate("settings", NavDestination.Settings)
                }
            }
        }
    }
}
