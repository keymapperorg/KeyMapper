package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.DeviceInfoRepository
import io.github.sds100.keymapper.data.KeymapRepository
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.util.BackupUtils
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.result.Failure
import io.github.sds100.keymapper.util.result.GenericFailure
import io.github.sds100.keymapper.util.result.handle
import io.github.sds100.keymapper.util.result.handleAsync
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

class BackupRestoreViewModel internal constructor(
    private val mKeymapRepository: KeymapRepository,
    private val mDeviceInfoRepository: DeviceInfoRepository
) : ViewModel() {

    val showMessageStringRes = MutableLiveData<Event<Int>>()
    val showErrorMessage = MutableLiveData<Event<Failure>>()
    val requestRestore = MutableLiveData<Event<Unit>>()

    fun backup(outputStream: OutputStream?, vararg keymapId: Long) {
        val keymaps = mKeymapRepository.keymapList.value?.filter { keymapId.contains(it.id) }
        backup(outputStream, keymaps)
    }

    fun backupAll(outputStream: OutputStream?) {
        backup(outputStream, mKeymapRepository.keymapList.value)
    }

    fun restore(inputStream: InputStream?) {
        if (inputStream == null) {
            showMessageStringRes.value = Event(R.string.error_failed_to_pick_file)
        }

        viewModelScope.launch {
            BackupUtils.restore(inputStream!!).handleAsync(
                onSuccess = {
                    mKeymapRepository.insertKeymap(*it.keymapList.toTypedArray())
                    mDeviceInfoRepository.insertDeviceInfo(*it.deviceInfo.toTypedArray())
                },
                onFailure = {
                    showErrorMessage.value = Event(it)

                    if (it is GenericFailure) {
                        Timber.e(it.exception)
                    }
                }
            )
        }
    }

    fun backup(outputStream: OutputStream?, keymapList: List<KeyMap>?) {
        if (outputStream == null) {
            showMessageStringRes.value = Event(R.string.error_failed_to_pick_file)
        }

        if (keymapList.isNullOrEmpty()) {
            showMessageStringRes.value = Event(R.string.error_no_keymaps)
            return
        }

        viewModelScope.launch {
            val deviceInfo = mDeviceInfoRepository.getAll()

            BackupUtils.backup(outputStream!!, keymapList, deviceInfo).handle(
                onSuccess = {
                    showMessageStringRes.value = Event(R.string.toast_backup_successful)
                },
                onFailure = {
                    showErrorMessage.value = Event(it)
                })
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val mKeymapRepository: KeymapRepository,
        private val mDeviceInfoRepository: DeviceInfoRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return BackupRestoreViewModel(mKeymapRepository, mDeviceInfoRepository) as T
        }
    }
}