package io.github.sds100.keymapper.actions.sound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.onSuccess
import io.github.sds100.keymapper.util.ui.DefaultSimpleListItem
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.showPopup
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 31/03/2020.
 */
class ChooseSoundFileViewModel(
    resourceProvider: ResourceProvider,
    private val useCase: ChooseSoundFileUseCase,
) : ViewModel(),
    PopupViewModel by PopupViewModelImpl(),
    ResourceProvider by resourceProvider {

    private val _chooseSoundFile = MutableSharedFlow<Unit>()
    val chooseSoundFile = _chooseSoundFile.asSharedFlow()

    private val _chooseSystemRingtone = MutableSharedFlow<Unit>()
    val chooseSystemRingtone = _chooseSystemRingtone.asSharedFlow()

    val soundFileListItems: StateFlow<List<DefaultSimpleListItem>> =
        useCase.soundFiles.map { sounds ->
            sounds.map {
                DefaultSimpleListItem(id = it.uid, title = it.name)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val returnResult: MutableStateFlow<ActionData.Sound?> =
        MutableStateFlow<ActionData.Sound?>(null)

    fun onChooseSoundFileButtonClick() {
        viewModelScope.launch {
            _chooseSoundFile.emit(Unit)
        }
    }

    fun onChooseSystemRingtoneButtonClick() {
        viewModelScope.launch {
            _chooseSystemRingtone.emit(Unit)
        }
    }

    fun onFileListItemClick(id: String) {
        viewModelScope.launch {
            val soundFileInfo = useCase.soundFiles.value.find { it.uid == id } ?: return@launch

            val dialog = PopupUi.Text(
                hint = getString(R.string.hint_sound_file_description),
                allowEmpty = false,
                text = soundFileInfo.name,
            )

            val soundDescription = showPopup("file_description", dialog) ?: return@launch

            returnResult.update {
                ActionData.Sound.SoundFile(
                    soundUid = soundFileInfo.uid,
                    soundDescription = soundDescription,
                )
            }
        }
    }

    fun onChooseNewSoundFile(uri: String) {
        viewModelScope.launch {
            val fileName = useCase.getSoundFileName(uri).valueOrNull() ?: return@launch

            val dialog = PopupUi.Text(
                hint = getString(R.string.hint_sound_file_description),
                allowEmpty = false,
                text = fileName,
            )

            val soundDescription = showPopup("file_description", dialog)

            soundDescription ?: return@launch

            useCase.saveSound(uri)
                .onSuccess { soundFileUid ->
                    returnResult.update {
                        ActionData.Sound.SoundFile(
                            soundFileUid,
                            soundDescription,
                        )
                    }
                }.onFailure { error ->
                    val toast = PopupUi.Toast(error.getFullMessage(this@ChooseSoundFileViewModel))
                    showPopup("failed_toast", toast)
                }
        }
    }

    fun onChooseRingtone(uri: String) {
        viewModelScope.launch {
            returnResult.update { ActionData.Sound.Ringtone(uri) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider,
        private val useCase: ChooseSoundFileUseCase,
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T = ChooseSoundFileViewModel(resourceProvider, useCase) as T
    }
}
