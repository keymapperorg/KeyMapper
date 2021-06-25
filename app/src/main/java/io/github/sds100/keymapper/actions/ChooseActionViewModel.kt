package io.github.sds100.keymapper.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.actions.sound.ChooseSoundFileResult
import io.github.sds100.keymapper.actions.sound.SoundsManager
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.getFullMessage
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
    private val soundsManager: SoundsManager
) : ViewModel(), PopupViewModel by PopupViewModelImpl() {

    var currentTabPosition: Int = 0

    private val _returnAction = MutableSharedFlow<ActionData>()
    val returnAction = _returnAction.asSharedFlow()

    fun onChooseSoundFile(result: ChooseSoundFileResult) {
        viewModelScope.launch {
            soundsManager.saveSound(result.uri)
                .onSuccess { soundFileUid ->
                    _returnAction.emit(SoundAction(soundFileUid, result.description))
                }.onFailure {
                    if (it is Error.Exception) {
                        it.exception.printStackTrace()
                    }
                    Timber.e(it.toString())
//todo
                }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val soundsManager: SoundsManager
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChooseActionViewModel(soundsManager) as T
        }
    }
}