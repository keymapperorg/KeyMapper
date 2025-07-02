package io.github.sds100.keymapper.base.keymaps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.ActionErrorSnapshot
import io.github.sds100.keymapper.base.backup.BackupRestoreMappingsUseCase
import io.github.sds100.keymapper.base.backup.ImportExportState
import io.github.sds100.keymapper.base.backup.RestoreType
import io.github.sds100.keymapper.base.constraints.ConstraintErrorSnapshot
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.constraints.ConstraintUiHelper
import io.github.sds100.keymapper.base.groups.Group
import io.github.sds100.keymapper.base.groups.GroupFamily
import io.github.sds100.keymapper.base.groups.GroupListItemModel
import io.github.sds100.keymapper.base.home.HomeWarningListItem
import io.github.sds100.keymapper.base.home.SelectedKeyMapsEnabled
import io.github.sds100.keymapper.base.home.ShowHomeScreenAlertsUseCase
import io.github.sds100.keymapper.base.onboarding.OnboardingTapTarget
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.base.sorting.SortViewModel
import io.github.sds100.keymapper.base.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.base.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.base.trigger.SetupGuiKeyboardState
import io.github.sds100.keymapper.base.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.base.trigger.TriggerError
import io.github.sds100.keymapper.base.trigger.TriggerErrorSnapshot
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogResponse
import io.github.sds100.keymapper.base.utils.ui.MultiSelectProvider
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.SelectionState
import io.github.sds100.keymapper.base.utils.ui.ViewModelHelper
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.common.utils.ifIsData
import io.github.sds100.keymapper.common.utils.mapData
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import io.github.sds100.keymapper.system.permissions.Permission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class KeyMapListViewModel(
    private val coroutineScope: CoroutineScope,
    private val listKeyMaps: ListKeyMapsUseCase,
    resourceProvider: ResourceProvider,
    private val setupGuiKeyboard: SetupGuiKeyboardUseCase,
    private val sortKeyMaps: SortKeyMapsUseCase,
    private val showAlertsUseCase: ShowHomeScreenAlertsUseCase,
    private val pauseKeyMaps: PauseKeyMapsUseCase,
    private val backupRestore: BackupRestoreMappingsUseCase,
    private val showInputMethodPickerUseCase: ShowInputMethodPickerUseCase,
    private val onboarding: OnboardingUseCase,
    private val navigationProvider: NavigationProvider,
    private val dialogProvider: DialogProvider
) : DialogProvider by dialogProvider,
    ResourceProvider by resourceProvider,
    NavigationProvider by navigationProvider {

    private companion object {
        const val ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM = "accessibility_service_disabled"
        const val ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM = "accessibility_service_crashed"
        const val ID_BATTERY_OPTIMISATION_LIST_ITEM = "battery_optimised"
        const val ID_LOGGING_ENABLED_LIST_ITEM = "logging_enabled"
        const val ID_NOTIFICATION_PERMISSION_DENIED_LIST_ITEM = "notification_permission_denied"
    }

    val sortViewModel = SortViewModel(coroutineScope, sortKeyMaps)
    var showSortBottomSheet by mutableStateOf(false)

    val multiSelectProvider: MultiSelectProvider = MultiSelectProvider()

    private val listItemCreator =
        KeyMapListItemCreator(
            listKeyMaps,
            resourceProvider,
        )
    private val constraintUiHelper = ConstraintUiHelper(listKeyMaps, resourceProvider)

    private val initialState =
        KeyMapListState(
            appBarState = KeyMapAppBarState.RootGroup(
                subGroups = emptyList(),
                warnings = emptyList(),
                isPaused = false,
            ),
            listItems = State.Loading,
            showCreateKeyMapTapTarget = false,
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

    private val isEditingGroupName = MutableStateFlow(false)

    private val warnings: Flow<List<HomeWarningListItem>> = combine(
        showAlertsUseCase.isBatteryOptimised,
        showAlertsUseCase.accessibilityServiceState,
        showAlertsUseCase.hideAlerts,
        showAlertsUseCase.isLoggingEnabled,
        showAlertsUseCase.showNotificationPermissionAlert,
    ) { isBatteryOptimised, serviceState, isHidden, isLoggingEnabled, showNotificationPermissionAlert ->
        if (isHidden) {
            return@combine emptyList()
        }

        buildList {
            when (serviceState) {
                AccessibilityServiceState.CRASHED ->
                    add(
                        HomeWarningListItem(
                            ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM,
                            getString(R.string.home_error_accessibility_service_is_crashed),
                        ),
                    )

                AccessibilityServiceState.DISABLED ->
                    add(
                        HomeWarningListItem(
                            ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM,
                            getString(R.string.home_error_accessibility_service_is_disabled),
                        ),
                    )

                AccessibilityServiceState.ENABLED -> {}
            }

            if (isBatteryOptimised) {
                add(
                    HomeWarningListItem(
                        ID_BATTERY_OPTIMISATION_LIST_ITEM,
                        getString(R.string.home_error_is_battery_optimised),
                    ),
                )
            } // don't show a success message for this

            if (showNotificationPermissionAlert) {
                add(
                    HomeWarningListItem(
                        ID_NOTIFICATION_PERMISSION_DENIED_LIST_ITEM,
                        getString(R.string.home_error_notification_permission),
                    ),
                )
            }

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

        val selectionGroupFamilyStateFlow = listKeyMaps.selectionGroupFamily.stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(5000),
            GroupFamily(null, emptyList(), emptyList()),
        )
        val selectionBreadcrumbs: Flow<List<GroupListItemModel>> =
            selectionGroupFamilyStateFlow.map { family ->
                family.parents.plus(family.group).filterNotNull()
                    .map { GroupListItemModel(uid = it.uid, name = it.name, icon = null) }
            }

        val selectionGroupListItems: Flow<List<GroupListItemModel>> =
            selectionGroupFamilyStateFlow.map { family ->
                family.children.map {
                    GroupListItemModel(
                        uid = it.uid,
                        name = it.name,
                        icon = null,
                    )
                }
            }

        val showThisGroup = combine(
            keyMapGroupStateFlow,
            selectionGroupFamilyStateFlow,
        ) { keyMapGroup, selectionGroup -> keyMapGroup.group?.uid != selectionGroup.group?.uid }

        val selectionAppBarState = combine(
            multiSelectProvider.state.filterIsInstance<SelectionState.Selecting>(),
            keyMapGroupStateFlow,
            selectionGroupListItems,
            selectionBreadcrumbs,
            showThisGroup,
        ) { selectionState, keyMapGroup, groups, selectionBreadcrumbs, showThisGroup ->
            buildSelectingAppBarState(
                keyMapGroup,
                selectionState,
                groups,
                selectionBreadcrumbs,
                showThisGroup,
            )
        }

        val groupAppBarState = combine(
            keyMapGroupStateFlow,
            warnings,
            pauseKeyMaps.isPaused,
            listKeyMaps.constraintErrorSnapshot,
            isEditingGroupName,
            transform = ::buildGroupAppBarState,
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        val appBarStateFlow: Flow<KeyMapAppBarState> =
            multiSelectProvider.state.flatMapLatest { selectionState ->
                when (selectionState) {
                    is SelectionState.Selecting -> selectionAppBarState
                    SelectionState.NotSelecting -> groupAppBarState
                }
            }

        val showCreateKeyMapTapTarget = combine(
            onboarding.showTapTarget(OnboardingTapTarget.CREATE_KEY_MAP),
            onboarding.showWhatsNew,
        ) { showTapTarget, showWhatsNew ->
            // Only show the tap target if whats new is not showing.
            showTapTarget && !showWhatsNew
        }

        coroutineScope.launch {
            combine(
                listItemStateFlow,
                appBarStateFlow,
                showCreateKeyMapTapTarget,
            ) { listState, appBarState, showCreateKeyMapTapTarget ->
                Triple(listState, appBarState, showCreateKeyMapTapTarget)
            }.collectLatest { (listState, appBarState, showCreateKeyMapTapTarget) ->
                listState.ifIsData { list ->
                    if (list.isNotEmpty()) {
                        showFabText = false
                    }
                }

                _state.value =
                    KeyMapListState(
                        appBarState,
                        listState,
                        showCreateKeyMapTapTarget,
                    )
            }
        }

        coroutineScope.launch {
            backupRestore.onAutomaticBackupResult.collectLatest { result ->
                onAutomaticBackupResult(result)
            }
        }
    }

    private fun buildSelectingAppBarState(
        keyMapGroup: KeyMapGroup,
        selectionState: SelectionState.Selecting,
        groupListItems: List<GroupListItemModel>,
        breadcrumbListItems: List<GroupListItemModel>,
        showThisGroup: Boolean,
    ): KeyMapAppBarState.Selecting {
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
            groups = groupListItems,
            breadcrumbs = breadcrumbListItems,
            showThisGroup = showThisGroup,
        )
    }

    private fun buildGroupAppBarState(
        keyMapGroup: KeyMapGroup,
        warnings: List<HomeWarningListItem>,
        isPaused: Boolean,
        constraintErrorSnapshot: ConstraintErrorSnapshot,
        isEditingGroupName: Boolean,
    ): KeyMapAppBarState {
        val subGroupListItems = keyMapGroup.subGroups.map { group ->
            buildGroupListItem(group)
        }

        val breadcrumbs = keyMapGroup.parents.plus(keyMapGroup.group).filterNotNull().map { group ->
            GroupListItemModel(
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
                parentConstraintCount = keyMapGroup.parents.sumOf { it.constraintState.constraints.size },
                subGroups = subGroupListItems,
                breadcrumbs = breadcrumbs,
                isEditingGroupName = isEditingGroupName,
                isNewGroup = isNewGroup,
            )
        }
    }

    private fun buildGroupListItem(group: Group): GroupListItemModel {
        var icon: ComposeIconInfo? = null

        val constraint = group.constraintState.constraints.firstOrNull()
        if (constraint != null) {
            icon = constraintUiHelper.getIcon(constraint)
        }

        return GroupListItemModel(
            uid = group.uid,
            name = group.name,
            icon = icon,
        )
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
                navigate("config_key_map", NavDestination.OpenKeyMap(uid))
            }
        }
    }

    fun onKeyMapCardLongClick(uid: String) {
        if (multiSelectProvider.state.value is SelectionState.NotSelecting) {
            coroutineScope.launch {
                multiSelectProvider.startSelecting()
                multiSelectProvider.select(uid)
            }
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
                        dialogProvider = this@KeyMapListViewModel,
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
                        NavDestination.NewKeyMap(
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
                SystemError.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY) -> {
                    ViewModelHelper.showDialogExplainingDndAccessBeingUnavailable(
                        resourceProvider = this@KeyMapListViewModel,
                        dialogProvider = this@KeyMapListViewModel,
                        neverShowDndTriggerErrorAgain = { listKeyMaps.neverShowDndTriggerError() },
                        fixError = { listKeyMaps.fixError(error) },
                    )
                }

                else -> {
                    ViewModelHelper.showFixErrorDialog(
                        resourceProvider = this@KeyMapListViewModel,
                        dialogProvider = this@KeyMapListViewModel,
                        error,
                    ) {
                        listKeyMaps.fixError(error)
                    }
                }
            }
        }
    }

    fun onEnableGuiKeyboardClick() {
        coroutineScope.launch {
            setupGuiKeyboard.enableInputMethod()
        }
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

    fun onSelectionGroupClick(groupUid: String?) {
        coroutineScope.launch {
            listKeyMaps.openSelectionGroup(groupUid)
        }
    }

    fun onMoveToThisGroupClick() {
        val selectionState = multiSelectProvider.state.value

        if (selectionState !is SelectionState.Selecting) return
        val selectedIds = selectionState.selectedIds.toTypedArray()

        listKeyMaps.moveKeyMapsToSelectedGroup(*selectedIds)

        multiSelectProvider.deselect(*selectedIds)
        multiSelectProvider.stopSelecting()
    }

    fun onFixWarningClick(id: String) {
        coroutineScope.launch {
            when (id) {
                ID_ACCESSIBILITY_SERVICE_DISABLED_LIST_ITEM -> {
                    val explanationResponse =
                        ViewModelHelper.showAccessibilityServiceExplanationDialog(
                            resourceProvider = this@KeyMapListViewModel,
                            dialogProvider = this@KeyMapListViewModel,
                        )

                    if (explanationResponse != DialogResponse.POSITIVE) {
                        return@launch
                    }

                    if (!showAlertsUseCase.startAccessibilityService()) {
                        ViewModelHelper.handleCantFindAccessibilitySettings(
                            resourceProvider = this@KeyMapListViewModel,
                            dialogProvider = this@KeyMapListViewModel,
                        )
                    }
                }

                ID_ACCESSIBILITY_SERVICE_CRASHED_LIST_ITEM ->
                    ViewModelHelper.handleKeyMapperCrashedDialog(
                        resourceProvider = this@KeyMapListViewModel,
                        dialogProvider = this@KeyMapListViewModel,
                        restartService = showAlertsUseCase::restartAccessibilityService,
                        ignoreCrashed = showAlertsUseCase::acknowledgeCrashed,
                    )

                ID_BATTERY_OPTIMISATION_LIST_ITEM -> showAlertsUseCase.disableBatteryOptimisation()
                ID_LOGGING_ENABLED_LIST_ITEM -> showAlertsUseCase.disableLogging()
                ID_NOTIFICATION_PERMISSION_DENIED_LIST_ITEM -> showNotificationPermissionAlertDialog()
            }
        }
    }

    private suspend fun showNotificationPermissionAlertDialog() {
        val dialog = DialogModel.Alert(
            title = getString(R.string.dialog_title_request_notification_permission),
            message = getText(R.string.dialog_message_request_notification_permission),
            positiveButtonText = getString(R.string.pos_turn_on),
            negativeButtonText = getString(R.string.neg_no_thanks),
            neutralButtonText = getString(R.string.pos_never_show_again),
        )

        val dialogResponse = showDialog("notification_permission_alert", dialog)

        if (dialogResponse == DialogResponse.POSITIVE) {
            showAlertsUseCase.requestNotificationPermission()
        } else if (dialogResponse == DialogResponse.NEUTRAL) {
            showAlertsUseCase.neverShowNotificationPermissionAlert()
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

    /**
     * @return whether the back was handled and the activity should not finish.
     */
    fun onBackClick(): Boolean {
        when {
            multiSelectProvider.state.value is SelectionState.Selecting -> {
                multiSelectProvider.stopSelecting()
                return true
            }

            state.value.appBarState is KeyMapAppBarState.ChildGroup -> {
                if (!isEditingGroupName.value) {
                    coroutineScope.launch {
                        listKeyMaps.popGroup()
                    }
                }

                isNewGroup = false
                isEditingGroupName.update { false }

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
                isNewGroup = false
                isEditingGroupName.update { false }
            }
        }
    }

    fun onEditGroupNameClick() {
        isNewGroup = false
        isEditingGroupName.update { true }
    }

    fun onGroupClick(uid: String?) {
        coroutineScope.launch {
            isNewGroup = false
            isEditingGroupName.update { false }
            listKeyMaps.openGroup(uid)
        }
    }

    fun onDeleteGroupClick() {
        coroutineScope.launch {
            isNewGroup = false
            isEditingGroupName.update { false }
            listKeyMaps.deleteGroup()
        }
    }

    fun onNewGroupClick() {
        coroutineScope.launch {
            // Must come first
            isNewGroup = true

            when (val selectionState = multiSelectProvider.state.value) {
                is SelectionState.Selecting ->
                    listKeyMaps.moveKeyMapsToNewGroup(*selectionState.selectedIds.toTypedArray())

                SelectionState.NotSelecting -> {
                    listKeyMaps.newGroup()
                }
            }

            multiSelectProvider.stopSelecting()
            isEditingGroupName.update { true }
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
                "config_new_key_map",
                NavDestination.NewKeyMap(groupUid = groupUid),
            )
        }
    }

    fun showInputMethodPicker() {
        showInputMethodPickerUseCase.show(fromForeground = true)
    }

    private suspend fun onAutomaticBackupResult(result: Result<*>) {
        when (result) {
            is Success -> {}

            is Error -> {
                val response = showDialog(
                    "automatic_backup_error",
                    DialogModel.Alert(
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

    fun onTapTargetsCompleted() {
        onboarding.completedTapTarget(OnboardingTapTarget.CREATE_KEY_MAP)
    }

    fun onSkipTapTargetClick() {
        onboarding.skipTapTargetOnboarding()
    }
}
