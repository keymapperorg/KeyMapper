package io.github.sds100.keymapper.mappings.keymaps

import androidx.lifecycle.ViewModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ui.*
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class KeyMapListViewModel constructor(
    private val coroutineScope: CoroutineScope,
    private val useCase: ListKeyMapsUseCase,
    resourceProvider: ResourceProvider,
    private val multiSelectProvider: MultiSelectProvider
) : ViewModel(), PopupViewModel by PopupViewModelImpl(), ResourceProvider by resourceProvider {

    private val listItemCreator = KeyMapListItemCreator(useCase, resourceProvider)

    private val _state = MutableStateFlow<State<List<KeyMapListItem>>>(State.Loading)
    val state = _state.asStateFlow()

    private val _launchConfigKeyMap = MutableSharedFlow<String>()
    val launchConfigKeymap = _launchConfigKeyMap.asSharedFlow()

    init {
        val keyMapStateListFlow =
            MutableStateFlow<State<List<KeyMapListItem.KeyMapUiState>>>(State.Loading)

        val rebuildUiState = MutableSharedFlow<State<List<KeyMap>>>(replay = 1)

        combine(
            rebuildUiState,
            useCase.showDeviceDescriptors
        ) { keyMapListState, showDeviceDescriptors ->
            keyMapStateListFlow.value = State.Loading


            keyMapStateListFlow.value = keyMapListState.mapData { keyMapList ->
                keyMapList.map { keyMap ->
                    listItemCreator.create(keyMap, showDeviceDescriptors)
                }
            }
        }.flowOn(Dispatchers.Default).launchIn(coroutineScope)

        coroutineScope.launch {
            useCase.keyMapList.collectLatest {
                rebuildUiState.emit(it)
            }
        }

        coroutineScope.launch {
            useCase.invalidateActionErrors.drop(1).collectLatest {
                /*
                Don't get the key maps from the repository because there can be a race condition
                when restoring key maps. This happens because when the activity is resumed the
                key maps in the repository are being updated and this flow is collected
                at the same time.
                 */
                rebuildUiState.emit(rebuildUiState.first())
            }
        }

        coroutineScope.launch {
            combine(
                keyMapStateListFlow,
                multiSelectProvider.state
            ) { keymapListState, selectionState ->
                Pair(keymapListState, selectionState)
            }.collectLatest { pair ->
                val (keyMapUiListState, selectionState) = pair

                _state.value = keyMapUiListState.mapData { keyMapUiList ->
                    val isSelectable = selectionState is SelectionState.Selecting

                    withContext(Dispatchers.Default) {
                        keyMapUiList.map { keymapUiState ->
                            val isSelected = if (selectionState is SelectionState.Selecting) {
                                selectionState.selectedIds.contains(keymapUiState.uid)
                            } else {
                                false
                            }

                            KeyMapListItem(
                                keymapUiState,
                                KeyMapListItem.SelectionUiState(isSelected, isSelectable)
                            )
                        }
                    }
                }
            }
        }
    }

    fun onKeymapCardClick(uid: String) {
        if (multiSelectProvider.state.value is SelectionState.Selecting) {
            multiSelectProvider.toggleSelection(uid)
        } else {
            coroutineScope.launch {
                _launchConfigKeyMap.emit(uid)
            }
        }
    }

    fun onKeymapCardLongClick(uid: String) {
        if (multiSelectProvider.state.value is SelectionState.NotSelecting) {
            multiSelectProvider.startSelecting()
            multiSelectProvider.select(uid)
        }
    }

    fun selectAll() {
        coroutineScope.launch {
            state.value.apply {
                if (this is State.Data) {
                    multiSelectProvider.select(*this.data.map { it.keyMapUiState.uid }
                        .toTypedArray())
                }
            }
        }
    }

    fun onTriggerErrorChipClick(chipModel: ChipUi) {
        if (chipModel is ChipUi.Error) {
            showSnackBarAndFixError(chipModel.error)
        }
    }

    fun onActionChipClick(chipModel: ChipUi) {
        if (chipModel is ChipUi.Error) {
            showSnackBarAndFixError(chipModel.error)
        }
    }

    fun onConstraintsChipClick(chipModel: ChipUi) {
        if (chipModel is ChipUi.Error) {
            showSnackBarAndFixError(chipModel.error)
        }
    }

    private fun showSnackBarAndFixError(error: Error) {
        coroutineScope.launch {
            val actionText = if (error.isFixable) {
                getString(R.string.snackbar_fix)
            } else {
                null
            }

            val snackBar = PopupUi.SnackBar(
                message = error.getFullMessage(this@KeyMapListViewModel),
                actionText = actionText
            )

            showPopup("fix_error", snackBar) ?: return@launch

            if (error.isFixable) {
                useCase.fixError(error)
            }
        }
    }
}