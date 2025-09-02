package io.github.sds100.keymapper.base.actions.sound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.ui.DefaultSimpleListItem
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.valueOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseSoundFileViewModel @Inject constructor(
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider,
    private val useCase: ChooseSoundFileUseCase,
) : ViewModel(),
    DialogProvider by dialogProvider,
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

            val dialog = DialogModel.Text(
                hint = getString(R.string.hint_sound_file_description),
                allowEmpty = false,
                text = soundFileInfo.name,
            )

            val soundDescription = showDialog("file_description", dialog) ?: return@launch

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

            val dialog = DialogModel.Text(
                hint = getString(R.string.hint_sound_file_description),
                allowEmpty = false,
                text = fileName,
            )

            val soundDescription = showDialog("file_description", dialog)

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
                    val toast = DialogModel.Toast(error.getFullMessage(this@ChooseSoundFileViewModel))
                    showDialog("failed_toast", toast)
                }
        }
    }

    fun onChooseRingtone(uri: String) {
        viewModelScope.launch {
            returnResult.update { ActionData.Sound.Ringtone(uri) }
        }
    }
}
