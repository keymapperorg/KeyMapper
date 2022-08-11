package io.github.sds100.keymapper.actions.sound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.onSuccess
import io.github.sds100.keymapper.util.ui.*
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by sds100 on 31/03/2020.
 */
@HiltViewModel
class ChooseSoundFileViewModel @Inject constructor(
    resourceProvider: ResourceProvider,
    private val useCase: ChooseSoundFileUseCase
) : ViewModel(), PopupViewModel by PopupViewModelImpl(), ResourceProvider by resourceProvider {

    private val _chooseSoundFile = MutableSharedFlow<Unit>()
    val chooseSoundFile = _chooseSoundFile.asSharedFlow()

    val soundFileListItems: StateFlow<List<DefaultSimpleListItem>> =
        useCase.soundFiles.map { sounds ->
            sounds.map {
                DefaultSimpleListItem(id = it.uid, title = it.name)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _returnResult = MutableSharedFlow<ChooseSoundResult>()
    val returnResult = _returnResult.asSharedFlow()

    fun onChooseSoundFileButtonClick() {
        viewModelScope.launch {
            _chooseSoundFile.emit(Unit)
        }
    }

    fun onFileListItemClick(id: String) {
        viewModelScope.launch {
            val soundFileInfo = useCase.soundFiles.value.find { it.uid == id } ?: return@launch

            val dialog = PopupUi.Text(
                hint = getString(R.string.choose_sound_file_description_title),
                allowEmpty = false,
                text = soundFileInfo.name
            )

            val soundDescription = showPopup("file_description", dialog) ?: return@launch

            _returnResult.emit(
                ChooseSoundResult(
                    soundUid = soundFileInfo.uid,
                    name = soundDescription
                )
            )
        }
    }

    fun onChooseNewSoundFile(uri: String) {
        viewModelScope.launch {
            val fileName = useCase.getSoundFileName(uri).valueOrNull() ?: return@launch

            val dialog = PopupUi.Text(
                hint = getString(R.string.choose_sound_file_description_title),
                allowEmpty = false,
                text = fileName
            )

            val soundDescription = showPopup("file_description", dialog)

            soundDescription ?: return@launch

            useCase.saveSound(uri, soundDescription)
                .onSuccess { soundFileUid ->
                    _returnResult.emit(ChooseSoundResult(soundFileUid, soundDescription))
                }.onFailure { error ->
                    val toast = PopupUi.Toast(error.getFullMessage(this@ChooseSoundFileViewModel))
                    showPopup("failed_toast", toast)
                }
        }
    }
}