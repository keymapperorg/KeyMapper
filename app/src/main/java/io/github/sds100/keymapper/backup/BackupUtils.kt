package io.github.sds100.keymapper.backup

import io.github.sds100.keymapper.system.files.FileUtils

object BackupUtils {

    const val DEFAULT_AUTOMATIC_BACKUP_NAME = "key_mapper.zip"

    fun createBackupFileName(): String {
        val formattedDate = FileUtils.createFileDate()
        return "key_maps_$formattedDate.zip"
    }
}
