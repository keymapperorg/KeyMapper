package io.github.sds100.keymapper.mappings.keymaps

import androidx.lifecycle.ViewModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.ui.*
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.MultiSelectProvider
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.isFixable
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
) : ViewModel(), PopupViewModel by PopupViewModelImpl(), ResourceProvider by resourceProvider{

    private val listItemCreator = KeyMapListItemCreator(useCase, resourceProvider)

    private val _state = MutableStateFlow<ListUiState<KeyMapListItem>>(ListUiState.Loading)
    val state = _state.asStateFlow()

    private val _launchConfigKeyMap = MutableSharedFlow<String>()
    val launchConfigKeymap = _launchConfigKeyMap.asSharedFlow()

    init {
        val keyMapStateListFlow =
            MutableStateFlow<ListUiState<KeyMapListItem.KeyMapUiState>>(ListUiState.Loading)

        val rebuildUiState = MutableSharedFlow<State<List<KeyMap>>>(replay = 1)

        coroutineScope.launch {
            rebuildUiState.collectLatest { keyMapListState ->

                keyMapStateListFlow.value = ListUiState.Loading

                keyMapStateListFlow.value = withContext(Dispatchers.Default) {
                    when (keyMapListState) {
                        is State.Data -> {
                            keyMapListState.data
                                .map { listItemCreator.map(it) }
                                .createListState()
                        }

                        State.Loading -> ListUiState.Loading
                    }
                }
            }
        }

        coroutineScope.launch {
            useCase.keyMapList.collectLatest {
                rebuildUiState.emit(it)
            }
        }

        coroutineScope.launch {
            useCase.invalidateErrors.drop(1).collectLatest {
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
                val (keymapUiListState, selectionState) = pair

                when (keymapUiListState) {
                    ListUiState.Empty -> _state.value = ListUiState.Empty
                    ListUiState.Loading -> _state.value = ListUiState.Loading

                    is ListUiState.Loaded -> {

                        val isSelectable = selectionState is SelectionState.Selecting

                        val listItems = withContext(Dispatchers.Default) {
                            keymapUiListState.data.map { keymapUiState ->
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

                        _state.value = listItems.createListState()
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
                if (this is ListUiState.Loaded) {
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
            val actionText = if (error.isFixable){
                getString(R.string.snackbar_fix)
            }else{
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