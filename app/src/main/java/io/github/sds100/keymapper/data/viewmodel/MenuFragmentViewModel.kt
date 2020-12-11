package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.repository.FingerprintMapRepository
import io.github.sds100.keymapper.data.usecase.MenuKeymapUseCase
import io.github.sds100.keymapper.util.FingerprintGestureUtils
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 17/11/20.
 */
class MenuFragmentViewModel(private val mKeymapUseCase: MenuKeymapUseCase,
                            private val mFingerprintMapRepository: FingerprintMapRepository) : ViewModel() {

    fun enableAll() = viewModelScope.launch {
        mKeymapUseCase.enableAll()

        FingerprintGestureUtils.GESTURES.forEach { gestureId ->
            mFingerprintMapRepository.editGesture(gestureId) {
                it.copy(isEnabled = true)
            }
        }
    }

    fun disableAll() = viewModelScope.launch {
        mKeymapUseCase.disableAll()

        FingerprintGestureUtils.GESTURES.forEach { gestureId ->
            mFingerprintMapRepository.editGesture(gestureId) {
                it.copy(isEnabled = false)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val mKeymapUseCase: MenuKeymapUseCase,
        private val mFingerprintMapRepository: FingerprintMapRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MenuFragmentViewModel(mKeymapUseCase, mFingerprintMapRepository) as T
        }
    }
}