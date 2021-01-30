package io.github.sds100.keymapper.util

/**
 * Created by sds100 on 17/06/2020.
 */
object BackupUtils {

    const val DEFAULT_AUTOMATIC_BACKUP_NAME = "keymapper_keymaps.json"

    fun createFileName(): String {
        val formattedDate = FileUtils.createFileDate()
        return "keymaps_$formattedDate.json"
    }
}