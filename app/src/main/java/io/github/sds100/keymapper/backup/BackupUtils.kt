package io.github.sds100.keymapper.backup

import io.github.sds100.keymapper.system.files.FileUtils

/**
 * Created by sds100 on 17/06/2020.
 */
object BackupUtils {

    const val DEFAULT_AUTOMATIC_BACKUP_NAME = "keymapper_mappings.json"

    fun createMappingsFileName(): String {
        val formattedDate = FileUtils.createFileDate()
        return "mappings_$formattedDate.json"
    }

    fun createKeyMapsFileName(): String {
        val formattedDate = FileUtils.createFileDate()
        return "key_maps_$formattedDate.json"
    }

    fun createFingerprintMapsFileName(): String {
        val formattedDate = FileUtils.createFileDate()
        return "fingerprint_maps_$formattedDate.json"
    }
}