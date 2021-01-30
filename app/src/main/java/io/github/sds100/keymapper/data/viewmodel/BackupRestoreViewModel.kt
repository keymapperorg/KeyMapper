package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.IBackupManager
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.*
import java.io.InputStream
import java.io.OutputStream

class BackupRestoreViewModel internal constructor(
    private val backupManager: IBackupManager
) : ViewModel() {

    private val _eventStream = LiveEvent<Event>().apply {
        addSource(backupManager.eventStream) {
            onBackupManagerEvent(it)
        }
    }
    val eventStream: LiveData<Event> = _eventStream

    fun backupAll(outputStream: OutputStream?) {
        if (outputStream == null) {
            _eventStream.value = MessageEvent(R.string.error_failed_to_pick_file)
            return
        }

        backupManager.backupEverything(outputStream)
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
            return
        }

        backupManager.restore(inputStream)
    }

    fun backupKeymaps(outputStream: OutputStream?, keymapIds: List<Long>) {
        if (keymapIds.isNullOrEmpty()) {
            _eventStream.value = MessageEvent(R.string.error_no_keymaps)
            return
        }

        if (outputStream == null) {
            _eventStream.value = MessageEvent(R.string.error_failed_to_pick_file)
            return
        }

        backupManager.backupKeymaps(outputStream, keymapIds)
    }

    fun backupFingerprintMaps(outputStream: OutputStream?) {
        if (outputStream == null) {
            _eventStream.value = MessageEvent(R.string.error_failed_to_pick_file)
            return
        }

        backupManager.backupFingerprintMaps(outputStream)
    }

    private fun onBackupManagerEvent(event: Event) {
        if (event is ResultEvent<*>) {
            event.result
                .onFailure {
                    _eventStream.value = when (it) {
                        is BackupVersionTooNew -> MessageEvent(R.string.error_backup_version_too_new)
                        is EmptyJson -> MessageEvent(R.string.error_empty_json)
                        is CorruptJsonFile -> MessageEvent(R.string.error_corrupt_json_file)
                        is FileAccessDenied -> AutomaticBackupResult(it)
                        else -> ShowErrorMessage(it)
                    }
                }

            event.result.onSuccess {
                when (event) {
                    is BackupResult ->
                        MessageEvent(R.string.toast_backup_successful)

                    is RestoreResult ->
                        MessageEvent(R.string.toast_restore_successful)

                    is AutomaticBackupResult ->
                        MessageEvent(R.string.toast_automatic_backup_successful)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val backupManager: IBackupManager
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return BackupRestoreViewModel(backupManager) as T
        }
    }
}