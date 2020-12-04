package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.usecase.BackupRestoreUseCase
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.GenericFailure
import io.github.sds100.keymapper.util.result.handle
import io.github.sds100.keymapper.util.result.handleAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

class BackupRestoreViewModel internal constructor(
    private val mKeymapRepository: BackupRestoreUseCase,
    private val mDeviceInfoRepository: DeviceInfoRepository
) : ViewModel() {

    private val _eventStream = LiveEvent<Event>()
    val eventStream: LiveData<Event> = _eventStream

    fun backup(outputStream: OutputStream?, vararg keymapId: Long) {
        viewModelScope.launch {
            val keymaps = withContext(Dispatchers.Default) {
                mKeymapRepository.getKeymaps().filter { keymapId.contains(it.id) }
            }

            backup(outputStream, keymaps)
        }
    }

    fun backupAll(outputStream: OutputStream?) {
        viewModelScope.launch {
            val keymaps = withContext(Dispatchers.Default) {
                mKeymapRepository.getKeymaps()
            }

            backup(outputStream, keymaps)
        }
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

    suspend fun backup(outputStream: OutputStream?, keymapList: List<KeyMap>?) {
        if (outputStream == null) {
            _eventStream.value = MessageEvent(R.string.error_failed_to_pick_file)
        }

        if (keymapList.isNullOrEmpty()) {
            _eventStream.value = MessageEvent(R.string.error_no_keymaps)
            return
        }

        val deviceInfo = mDeviceInfoRepository.getAll()

        BackupUtils.backup(outputStream!!, keymapList, deviceInfo).handle(
            onSuccess = {
                _eventStream.value = MessageEvent(R.string.toast_backup_successful)
            },
            onFailure = {
                _eventStream.value = ShowErrorMessage(it)
            })
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val mBackupRestoreUseCase: BackupRestoreUseCase,
        private val mDeviceInfoRepository: DeviceInfoRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return BackupRestoreViewModel(mBackupRestoreUseCase, mDeviceInfoRepository) as T
        }
    }
}