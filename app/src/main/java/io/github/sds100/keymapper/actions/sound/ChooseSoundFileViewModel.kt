package io.github.sds100.keymapper.actions.sound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.onSuccess
import io.github.sds100.keymapper.util.ui.*
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Created by sds100 on 31/03/2020.
 */
class ChooseSoundFileViewModel(
    resourceProvider: ResourceProvider,
    private val useCase: ChooseSoundFileUseCase
) : ViewModel(), PopupViewModel by PopupViewModelImpl(), ResourceProvider by resourceProvider {

    private val _chooseSoundFile = MutableSharedFlow<Unit>()
    val chooseSoundFile = _chooseSoundFile.asSharedFlow()

    val soundFileListItems: StateFlow<List<SoundFileInfo>> = useCase.soundFiles

    private val _returnResult = MutableSharedFlow<ChooseSoundFileResult>()
    val returnResult = _returnResult.asSharedFlow()

    fun onChooseSoundFileButtonClick() {
        viewModelScope.launch {
            _chooseSoundFile.emit(Unit)
        }
    }

    fun onFileListItemClick(soundFileInfo: SoundFileInfo) {
        viewModelScope.launch {
            val dialog = PopupUi.Text(
                hint = getString(R.string.hint_sound_file_description),
                allowEmpty = false,
                text = soundFileInfo.name
            )

            val result = showPopup("file_description", dialog)

            result ?: return@launch

            _returnResult.emit(
                ChooseSoundFileResult(
                    soundUid = soundFileInfo.uid,
                    description = result.text
                )
            )
        }
    }

    fun onChooseSoundFile(uri: String) {
        viewModelScope.launch {
            val fileName = useCase.getSoundFileName(uri).valueOrNull() ?: return@launch

            val dialog = PopupUi.Text(
                hint = getString(R.string.hint_sound_file_description),
                allowEmpty = false,
                text = fileName
            )

            val soundDescription = showPopup("file_description", dialog)?.text

            soundDescription ?: return@launch

            useCase.saveSound(uri)
                .onSuccess { soundFileUid ->
                    _returnResult.emit(ChooseSoundFileResult(soundFileUid, soundDescription))
                }.onFailure {
                    Timber.e(it.toString())

                    val toast = PopupUi.Toast(getString(R.string.toast_failed_to_choose_sound_file))
                    showPopup("failed_toast", toast)
                }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider,
        private val useCase: ChooseSoundFileUseCase
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChooseSoundFileViewModel(resourceProvider, useCase) as T
        }
    }
}