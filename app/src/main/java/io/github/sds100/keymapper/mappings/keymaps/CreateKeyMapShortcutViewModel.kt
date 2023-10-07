package io.github.sds100.keymapper.mappings.keymaps

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 08/09/20.
 */
class CreateKeyMapShortcutViewModel(
    private val configKeyMapUseCase: ConfigKeyMapUseCase,
    private val listUseCase: ListKeyMapsUseCase,
    private val createShortcutUseCase: CreateKeyMapShortcutUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(), PopupViewModel by PopupViewModelImpl(),
    ResourceProvider by resourceProvider {

    private val listItemCreator = KeyMapListItemCreator(listUseCase, resourceProvider)

    private val _state = MutableStateFlow<State<List<KeyMapListItem>>>(State.Loading)
    val state = _state.asStateFlow()

    private val _returnIntentResult = MutableSharedFlow<Intent>()
    val returnUidResult = _returnIntentResult.asSharedFlow()

    init {
        val rebuildUiState = MutableSharedFlow<State<List<KeyMap>>>(replay = 1)

        combine(
            rebuildUiState,
            listUseCase.showDeviceDescriptors
        ) { keyMapListState, showDeviceDescriptors ->
            val selectionUiState =
                KeyMapListItem.SelectionUiState(isSelected = false, isSelectable = false)

            _state.value = keyMapListState.mapData { keyMapList ->
                keyMapList.map { keyMap ->
                    val keyMapListUiState = listItemCreator.create(keyMap, showDeviceDescriptors)

                    KeyMapListItem(keyMapListUiState, selectionUiState)
                }
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            listUseCase.keyMapList.collectLatest {
                rebuildUiState.emit(it)
            }
        }

        viewModelScope.launch {
            listUseCase.invalidateActionErrors.drop(1).collectLatest {
                /*
                Don't get the key maps from the repository because there can be a race condition
                when restoring key maps. This happens because when the activity is resumed the
                key maps in the repository are being updated and this flow is collected
                at the same time.
                 */
                rebuildUiState.emit(rebuildUiState.first())
            }
        }
    }

    fun onKeyMapCardClick(uid: String) {
        viewModelScope.launch {
            val state = listUseCase.keyMapList.first { it is State.Data }

            if (state !is State.Data) return@launch

            configKeyMapUseCase.loadKeyMap(uid)
            configKeyMapUseCase.setTriggerFromOtherAppsEnabled(true)

            val keyMapState = configKeyMapUseCase.mapping.first()

            if (keyMapState !is State.Data) return@launch

            val keyMap = keyMapState.data

            val intent = if (keyMap.actionList.size == 1) {
                createShortcutUseCase.createIntentForSingleAction(keyMap.uid, keyMap.actionList[0])
            } else {

                val key = "create_launcher_shortcut"
                val shortcutName = showPopup(
                    key,
                    PopupUi.Text(
                        getString(R.string.hint_shortcut_name),
                        allowEmpty = false
                    )
                ) ?: return@launch

                createShortcutUseCase.createIntentForMultipleActions(
                    keyMapUid = keyMap.uid,
                    shortcutLabel = shortcutName
                )
            }

            configKeyMapUseCase.save()

            _returnIntentResult.emit(intent)
        }
    }

    fun onTriggerErrorChipClick(chipModel: ChipUi) {
        if (chipModel is ChipUi.Error) {
            showDialogAndFixError(chipModel.error)
        }
    }

    fun onActionChipClick(chipModel: ChipUi) {
        if (chipModel is ChipUi.Error) {
            showDialogAndFixError(chipModel.error)
        }
    }

    fun onConstraintsChipClick(chipModel: ChipUi) {
        if (chipModel is ChipUi.Error) {
            showDialogAndFixError(chipModel.error)
        }
    }

    private fun showDialogAndFixError(error: Error) {
        viewModelScope.launch {
            ViewModelHelper.showFixErrorDialog(
                resourceProvider = this@CreateKeyMapShortcutViewModel,
                popupViewModel = this@CreateKeyMapShortcutViewModel,
                error
            ) {
                listUseCase.fixError(error)
            }
        }
    }

    class Factory(
        private val configKeyMapUseCase: ConfigKeyMapUseCase,
        private val listUseCase: ListKeyMapsUseCase,
        private val createShortcutUseCase: CreateKeyMapShortcutUseCase,
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            CreateKeyMapShortcutViewModel(
                configKeyMapUseCase, listUseCase, createShortcutUseCase, resourceProvider
            ) as T
    }
}