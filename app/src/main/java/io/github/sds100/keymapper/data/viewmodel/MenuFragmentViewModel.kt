package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.repository.FingerprintMapRepository
import io.github.sds100.keymapper.data.usecase.MenuKeymapUseCase
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 17/11/20.
 */
class MenuFragmentViewModel(private val keymapUseCase: MenuKeymapUseCase,
                            private val fingerprintMapRepository: FingerprintMapRepository
) : ViewModel() {

    val keymapsPaused = MutableLiveData(false)
    val accessibilityServiceEnabled = MutableLiveData(false)

    private val _eventStream = LiveEvent<Event>()
    val eventStream: LiveData<Event> = _eventStream

    fun enableAll() = viewModelScope.launch {
        keymapUseCase.enableAll()

        FingerprintMapUtils.GESTURES.forEach { gestureId ->
            fingerprintMapRepository.updateGesture(gestureId) {
                it.copy(isEnabled = true)
            }
        }
    }

    fun disableAll() = viewModelScope.launch {
        keymapUseCase.disableAll()

        FingerprintMapUtils.GESTURES.forEach { gestureId ->
            fingerprintMapRepository.updateGesture(gestureId) {
                it.copy(isEnabled = false)
            }
        }
    }

    fun chooseKeyboard() = run { _eventStream.value = ChooseKeyboard() }
    fun openSettings() = run { _eventStream.value = OpenSettings() }
    fun openAbout() = run { _eventStream.value = OpenAbout() }
    fun sendFeedback() = run { _eventStream.value = SendFeedback() }
    fun backupAll() = run { _eventStream.value = RequestBackupAll() }
    fun restore() = run { _eventStream.value = RequestRestore() }
    fun resumeKeymaps() = run { _eventStream.value = ResumeKeymaps() }
    fun pauseKeymaps() = run { _eventStream.value = PauseKeymaps() }
    fun enableAccessibilityService() = run { _eventStream.value = EnableAccessibilityService() }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val keymapUseCase: MenuKeymapUseCase,
        private val fingerprintMapRepository: FingerprintMapRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MenuFragmentViewModel(keymapUseCase, fingerprintMapRepository) as T
        }
    }
}