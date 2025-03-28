package io.github.sds100.keymapper.mappings.keymaps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.sds100.keymapper.actions.ActionErrorSnapshot
import io.github.sds100.keymapper.constraints.ConstraintErrorSnapshot
import io.github.sds100.keymapper.groups.SubGroupListModel
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.mappings.keymaps.trigger.SetupGuiKeyboardState
import io.github.sds100.keymapper.mappings.keymaps.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerError
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerErrorSnapshot
import io.github.sds100.keymapper.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.MultiSelectProvider
import io.github.sds100.keymapper.util.ui.NavDestination
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SelectionState
import io.github.sds100.keymapper.util.ui.ViewModelHelper
import io.github.sds100.keymapper.util.ui.navigate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeyMapListViewModel(
    private val coroutineScope: CoroutineScope,
    private val listKeyMaps: ListKeyMapsUseCase,
    resourceProvider: ResourceProvider,
    private val multiSelectProvider: MultiSelectProvider,
    private val setupGuiKeyboard: SetupGuiKeyboardUseCase,
    private val sortKeyMaps: SortKeyMapsUseCase,
) : PopupViewModel by PopupViewModelImpl(),
    ResourceProvider by resourceProvider,
    NavigationViewModel by NavigationViewModelImpl() {

    private val listItemCreator = KeyMapListItemCreator(listKeyMaps, resourceProvider)

    private val _state = MutableStateFlow<KeyMapListState>(KeyMapListState.Root())
    val state = _state.asStateFlow()

    var showFabText: Boolean by mutableStateOf(true)

    val isSelectable: StateFlow<Boolean> =
        multiSelectProvider.state.map { it is SelectionState.Selecting }
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

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
        ),
    )

    init {
        val keyMapGroupFlow = combine(
            keyMapGroupStateFlow,
            sortKeyMaps.observeKeyMapsSorter(),
        ) { keyMapGroup, sorter ->
            keyMapGroup.copy(
                keyMaps = keyMapGroup.keyMaps.mapData { list -> list.sortedWith(sorter) },
            )
        }.flowOn(Dispatchers.Default)

        val listStateFlow =
            combine(
                keyMapGroupFlow,
                listKeyMaps.showDeviceDescriptors,
                listKeyMaps.triggerErrorSnapshot,
                listKeyMaps.actionErrorSnapshot,
                listKeyMaps.constraintErrorSnapshot,
                transform = ::buildState,
            ).flowOn(Dispatchers.Default)

        // The list item content should be separate from the selection state
        // because creating the content is an expensive operation and selection should be almost
        // instantaneous.
        coroutineScope.launch(Dispatchers.Default) {
            combine(
                listStateFlow,
                multiSelectProvider.state,
            ) { listState, selectionState ->
                Pair(listState, selectionState)
            }.collectLatest { (listState, selectionState) ->
                // Stop selecting when there are no key maps
                listState.listItems.ifIsData { list ->
                    if (list.isEmpty()) {
                        multiSelectProvider.stopSelecting()
                    }
                }

                showFabText = listState.listItems.dataOrNull()?.isEmpty() ?: true

                val listItemsWithSelection = listState.listItems.mapData { listItems ->
                    listItems.map { item ->
                        val isSelected = if (selectionState is SelectionState.Selecting) {
                            selectionState.selectedIds.contains(item.uid)
                        } else {
                            false
                        }

                        item.copy(isSelected = isSelected)
                    }
                }

                _state.value = when (listState) {
                    is KeyMapListState.Root -> listState.copy(listItems = listItemsWithSelection)
                    is KeyMapListState.Child -> listState.copy(listItems = listItemsWithSelection)
                }
            }
        }
    }

    private fun buildState(
        keyMapGroup: KeyMapGroup,
        showDeviceDescriptors: Boolean,
        triggerErrorSnapshot: TriggerErrorSnapshot,
        actionErrorSnapshot: ActionErrorSnapshot,
        constraintErrorSnapshot: ConstraintErrorSnapshot,
    ): KeyMapListState {
        val listItemsState = keyMapGroup.keyMaps.mapData { list ->
            list.map {
                val content = listItemCreator.build(
                    it,
                    showDeviceDescriptors,
                    triggerErrorSnapshot,
                    actionErrorSnapshot,
                    constraintErrorSnapshot,
                )

                KeyMapListItemModel(isSelected = false, content)
            }
        }

        val subGroupListItems = keyMapGroup.subGroups.map { group ->
            SubGroupListModel(
                uid = group.uid,
                name = group.name,
                icon = null, // TODO show icon depending on constraints
            )
        }

        if (keyMapGroup.group == null) {
            return KeyMapListState.Root(
                subGroups = subGroupListItems,
                listItems = listItemsState,
            )
        } else {
            return KeyMapListState.Child(
                groupName = keyMapGroup.group.name,
                constraints = listItemCreator.buildConstraintChipList(
                    keyMapGroup.group.constraintState,
                    constraintErrorSnapshot,
                ),
                constraintMode = keyMapGroup.group.constraintState.mode,
                subGroups = subGroupListItems,
                listItems = listItemsState,
            )
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
                navigate("config_key_map", NavDestination.ConfigKeyMap(uid))
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

    fun selectAll() {
        coroutineScope.launch {
            state.value.listItems.apply {
                if (this is State.Data) {
                    multiSelectProvider.select(
                        *this.data.map { it.uid }.toTypedArray(),
                    )
                }
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
                        NavDestination.ConfigKeyMap(
                            keyMapUid = null,
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
}
