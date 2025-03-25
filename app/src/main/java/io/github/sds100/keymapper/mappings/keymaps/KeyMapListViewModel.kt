package io.github.sds100.keymapper.mappings.keymaps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.mappings.keymaps.trigger.SetupGuiKeyboardState
import io.github.sds100.keymapper.mappings.keymaps.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerError
import io.github.sds100.keymapper.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.State
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

    private val _state = MutableStateFlow<State<List<KeyMapListItemModel>>>(State.Loading)
    val state = _state.asStateFlow()

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

    init {
        val keyMapListFlow = combine(
            listKeyMaps.keyMapList,
            sortKeyMaps.observeKeyMapsSorter(),
        ) { keyMapList, sorter ->
            keyMapList.mapData { list -> list.sortedWith(sorter) }
        }.flowOn(Dispatchers.Default)

        val listItemContentFlow =
            combine(
                keyMapListFlow,
                listKeyMaps.showDeviceDescriptors,
                listKeyMaps.triggerErrorSnapshot,
                listKeyMaps.actionErrorSnapshot,
                listKeyMaps.constraintErrorSnapshot,
            ) { keyMapListState, showDeviceDescriptors, triggerErrorSnapshot, actionErrorSnapshot, constraintErrorSnapshot ->
                keyMapListState.mapData { keyMapList ->
                    keyMapList.map { keyMap ->
                        listItemCreator.create(
                            keyMap,
                            showDeviceDescriptors,
                            triggerErrorSnapshot,
                            actionErrorSnapshot,
                            constraintErrorSnapshot,
                        )
                    }
                }
            }.flowOn(Dispatchers.Default)

        // The list item content should be separate from the selection state
        // because creating the content is an expensive operation and selection should be almost
        // instantaneous.
        coroutineScope.launch(Dispatchers.Default) {
            combine(
                listItemContentFlow,
                multiSelectProvider.state,
            ) { keymapListState, selectionState ->
                Pair(keymapListState, selectionState)
            }.collectLatest { pair ->
                val (listItemContentList, selectionState) = pair

                _state.value = listItemContentList.mapData { contentList ->
                    contentList.map { content ->
                        val isSelected = if (selectionState is SelectionState.Selecting) {
                            selectionState.selectedIds.contains(content.uid)
                        } else {
                            false
                        }

                        KeyMapListItemModel(isSelected, content)
                    }
                }
            }
        }
    }

    fun onKeyMapCardClick(uid: String) {
        if (multiSelectProvider.state.value is SelectionState.Selecting) {
            multiSelectProvider.toggleSelection(uid)
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
        }
    }

    fun selectAll() {
        coroutineScope.launch {
            state.value.apply {
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
                        NavDestination.ConfigKeyMap(keyMapUid = null, showAdvancedTriggers = true),
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
