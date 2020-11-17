package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.repository.FingerprintGestureRepository
import io.github.sds100.keymapper.data.usecase.MenuKeymapUseCase
import io.github.sds100.keymapper.util.FingerprintGestureUtils
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 17/11/20.
 */
class MenuFragmentViewModel(private val mKeymapUseCase: MenuKeymapUseCase,
                            private val mFingerprintGestureRepository: FingerprintGestureRepository) : ViewModel() {

    fun enableAll() {
        viewModelScope.launch {
            mKeymapUseCase.enableAll()
        }

        FingerprintGestureUtils.GESTURES.forEach { gestureId ->
            mFingerprintGestureRepository.edit(gestureId) {
                it.clone(isEnabled = true)
            }
        }
    }

    fun disableAll() {
        viewModelScope.launch {
            mKeymapUseCase.disableAll()
        }

        FingerprintGestureUtils.GESTURES.forEach { gestureId ->
            mFingerprintGestureRepository.edit(gestureId) {
                it.clone(isEnabled = false)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val mKeymapUseCase: MenuKeymapUseCase,
        private val mFingerprintGestureRepository: FingerprintGestureRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MenuFragmentViewModel(mKeymapUseCase, mFingerprintGestureRepository) as T
        }
    }
}