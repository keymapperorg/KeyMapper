package io.github.sds100.keymapper.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.actions.sound.ChooseSoundFileResult
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 30/03/2020.
 */

class ChooseActionViewModel : ViewModel(), PopupViewModel by PopupViewModelImpl() {

    var currentTabPosition: Int = 0

    private val _returnAction = MutableSharedFlow<ActionData>()
    val returnAction = _returnAction.asSharedFlow()

    fun onChooseSoundFile(result: ChooseSoundFileResult) {
        viewModelScope.launch {
            _returnAction.emit(SoundAction(result.soundUid, result.description))
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChooseActionViewModel() as T
        }
    }
}