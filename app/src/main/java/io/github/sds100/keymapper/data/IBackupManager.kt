package io.github.sds100.keymapper.data

import androidx.lifecycle.LiveData
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.result.Result
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by sds100 on 23/01/21.
 */
interface IBackupManager {
    val eventStream: LiveData<Event>
    fun backupKeymaps(outputStream: OutputStream, keymapIds: List<Long>)
    fun backupFingerprintMaps(outputStream: OutputStream)
    fun backupEverything(outputStream: OutputStream)
    fun restore(inputStream: InputStream)
}