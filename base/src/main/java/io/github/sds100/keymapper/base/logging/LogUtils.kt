package io.github.sds100.keymapper.base.logging

import io.github.sds100.keymapper.data.entities.LogEntryEntity
import io.github.sds100.keymapper.system.files.FileUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogUtils {
    val DATE_FORMAT
        get() = SimpleDateFormat("MM/dd HH:mm:ss.SSS", Locale.getDefault())

    fun createLogFileName(): String {
        val formattedDate = FileUtils.createFileDate()
        return "key_mapper_log_$formattedDate.txt"
    }

    fun createLogText(logEntries: List<LogEntryEntity>): String {
        val dateFormat = DATE_FORMAT

        return logEntries.joinToString(separator = "\n") { entry ->
            val date = dateFormat.format(Date(entry.time))

            return@joinToString "$date  ${entry.message}"
        }
    }
}
