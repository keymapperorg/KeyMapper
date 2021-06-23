package io.github.sds100.keymapper.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.actions.sound.ChooseSoundFileResult
import io.github.sds100.keymapper.actions.sound.CreateSoundActionUseCase
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.onSuccess
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Created by sds100 on 30/03/2020.
 */

class ChooseActionViewModel(
    private val createSoundActionUseCase: CreateSoundActionUseCase
) : ViewModel(), PopupViewModel by PopupViewModelImpl() {

    var currentTabPosition: Int = 0

    private val _returnAction = MutableSharedFlow<ActionData>()
    val returnAction = _returnAction.asSharedFlow()

    fun onChooseSoundFile(result: ChooseSoundFileResult) {
        viewModelScope.launch {
            createSoundActionUseCase.storeSoundFile(result.uri).apply {
            }.onSuccess { soundFileUid ->
                _returnAction.emit(SoundAction(soundFileUid, result.description))
            }.onFailure {
                Timber.e(it.toString())
//todo
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val createSoundActionUseCase: CreateSoundActionUseCase
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChooseActionViewModel(createSoundActionUseCase) as T
        }
    }
}