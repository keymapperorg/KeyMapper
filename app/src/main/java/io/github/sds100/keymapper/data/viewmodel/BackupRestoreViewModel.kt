package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.FingerprintMap
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.repository.FingerprintMapRepository
import io.github.sds100.keymapper.data.usecase.BackupRestoreUseCase
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.FingerprintMapUtils.SWIPE_DOWN
import io.github.sds100.keymapper.util.FingerprintMapUtils.SWIPE_LEFT
import io.github.sds100.keymapper.util.FingerprintMapUtils.SWIPE_RIGHT
import io.github.sds100.keymapper.util.FingerprintMapUtils.SWIPE_UP
import io.github.sds100.keymapper.util.result.GenericFailure
import io.github.sds100.keymapper.util.result.handle
import io.github.sds100.keymapper.util.result.handleAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

class BackupRestoreViewModel internal constructor(
    private val mKeymapRepository: BackupRestoreUseCase,
    private val mDeviceInfoRepository: DeviceInfoRepository,
    private val mFingerprintMapRepository: FingerprintMapRepository
) : ViewModel() {

    private val _eventStream = LiveEvent<Event>()
    val eventStream: LiveData<Event> = _eventStream

    fun backupAll(outputStream: OutputStream?) {
        viewModelScope.launch {
            val keymaps = withContext(Dispatchers.Default) {
                mKeymapRepository.getKeymaps()
            }

            backup(outputStream, keymaps,
                mFingerprintMapRepository.swipeDown.firstOrNull(),
                mFingerprintMapRepository.swipeUp.firstOrNull(),
                mFingerprintMapRepository.swipeLeft.firstOrNull(),
                mFingerprintMapRepository.swipeRight.firstOrNull())
        }
    }

    fun requestBackupAll() {
        _eventStream.value = RequestBackupAll()
    }

    fun requestRestore() {
        _eventStream.value = RequestRestore()
    }

    fun restore(inputStream: InputStream?) {
        if (inputStream == null) {
            _eventStream.value = MessageEvent(R.string.error_failed_to_pick_file)
        }

        viewModelScope.launch {
            BackupUtils.restore(inputStream!!).handleAsync(
                onSuccess = {
                    mKeymapRepository.insertKeymap(*it.keymapList.toTypedArray())
                    mDeviceInfoRepository.insertDeviceInfo(*it.deviceInfo.toTypedArray())

                    restoreFingerprintMap(SWIPE_DOWN, it.fingerprintSwipeDown)
                    restoreFingerprintMap(SWIPE_UP, it.fingerprintSwipeUp)
                    restoreFingerprintMap(SWIPE_LEFT, it.fingerprintSwipeLeft)
                    restoreFingerprintMap(SWIPE_RIGHT, it.fingerprintSwipeRight)
                },
                onFailure = {
                    _eventStream.value = ShowErrorMessage(it)

                    if (it is GenericFailure) {
                        Timber.e(it.exception)
                    }
                }
            )
        }
    }

    fun backupKeymaps(outputStream: OutputStream?, keymaps: List<KeyMap>) {
        if (keymaps.isNullOrEmpty()) {
            _eventStream.value = MessageEvent(R.string.error_no_keymaps)
            return
        }

        viewModelScope.launch {
            backup(outputStream, keymaps)
        }
    }

    fun backupFingerprintMaps(outputStream: OutputStream?) {
        viewModelScope.launch {
            backup(
                outputStream,
                emptyList(),
                mFingerprintMapRepository.swipeDown.firstOrNull(),
                mFingerprintMapRepository.swipeUp.firstOrNull(),
                mFingerprintMapRepository.swipeLeft.firstOrNull(),
                mFingerprintMapRepository.swipeRight.firstOrNull())
        }
    }

    private suspend fun backup(
        outputStream: OutputStream?,
        keymapList: List<KeyMap> = emptyList(),
        fingerprintSwipeDown: FingerprintMap? = null,
        fingerprintSwipeUp: FingerprintMap? = null,
        fingerprintSwipeLeft: FingerprintMap? = null,
        fingerprintSwipeRight: FingerprintMap? = null) {

        if (outputStream == null) {
            _eventStream.value = MessageEvent(R.string.error_failed_to_pick_file)
        }

        val deviceInfo = mDeviceInfoRepository.getAll()

        BackupUtils.backup(
            outputStream!!,
            keymapList,
            deviceInfo,
            fingerprintSwipeDown,
            fingerprintSwipeUp,
            fingerprintSwipeLeft,
            fingerprintSwipeRight
        ).handle(
            onSuccess = {
                _eventStream.value = MessageEvent(R.string.toast_backup_successful)
            },
            onFailure = {
                _eventStream.value = ShowErrorMessage(it)
            })
    }

    private suspend fun restoreFingerprintMap(gestureId: String, fingerprintMap: FingerprintMap?) {
        fingerprintMap ?: return

        mFingerprintMapRepository.editGesture(gestureId) {
            fingerprintMap
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val mBackupRestoreUseCase: BackupRestoreUseCase,
        private val mDeviceInfoRepository: DeviceInfoRepository,
        private val mFingerprintMapRepository: FingerprintMapRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return BackupRestoreViewModel(
                mBackupRestoreUseCase,
                mDeviceInfoRepository,
                mFingerprintMapRepository
            ) as T
        }
    }
}