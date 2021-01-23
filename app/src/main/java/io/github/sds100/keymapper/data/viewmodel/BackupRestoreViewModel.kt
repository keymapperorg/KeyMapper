package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.db.AppDatabase
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
import io.github.sds100.keymapper.util.result.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

class BackupRestoreViewModel internal constructor(
    private val keymapRepository: BackupRestoreUseCase,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val fingerprintMapRepository: FingerprintMapRepository
) : ViewModel() {

    private val _eventStream = LiveEvent<Event>()
    val eventStream: LiveData<Event> = _eventStream

    fun backupAll(outputStream: OutputStream?) {
        viewModelScope.launch {
            val keymaps = withContext(Dispatchers.Default) {
                keymapRepository.getKeymaps()
            }

            backup(outputStream, keymaps,
                fingerprintMapRepository.swipeDown.firstOrNull(),
                fingerprintMapRepository.swipeUp.firstOrNull(),
                fingerprintMapRepository.swipeLeft.firstOrNull(),
                fingerprintMapRepository.swipeRight.firstOrNull())
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
                    keymapRepository.insertKeymap(it.keymapDbVersion, *it.keymapList.toTypedArray())
                        .onFailure { failure ->
                            if (failure is IncompatibleBackup) {
                                _eventStream.value = MessageEvent(R.string.error_incompatible_backup)
                            }
                        }

                    deviceInfoRepository.insertDeviceInfo(*it.deviceInfo.toTypedArray())

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
                fingerprintMapRepository.swipeDown.firstOrNull(),
                fingerprintMapRepository.swipeUp.firstOrNull(),
                fingerprintMapRepository.swipeLeft.firstOrNull(),
                fingerprintMapRepository.swipeRight.firstOrNull())
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

        val deviceInfo = deviceInfoRepository.getAll()

        BackupUtils.backup(
            outputStream!!,
            AppDatabase.DATABASE_VERSION,
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

//        fingerprintMapRepository.rest(gestureId, fingerprintMap).onFailure {
//            if (it is IncompatibleBackup) {
//                _eventStream.value = MessageEvent(R.string.error_incompatible_backup)
//            }
//        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val backupRestoreUseCase: BackupRestoreUseCase,
        private val deviceInfoRepository: DeviceInfoRepository,
        private val fingerprintMapRepository: FingerprintMapRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return BackupRestoreViewModel(
                backupRestoreUseCase,
                deviceInfoRepository,
                fingerprintMapRepository
            ) as T
        }
    }
}