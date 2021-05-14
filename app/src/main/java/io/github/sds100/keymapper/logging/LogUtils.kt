package io.github.sds100.keymapper.logging

import io.github.sds100.keymapper.system.files.FileUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by sds100 on 14/05/2021.
 */
object LogUtils {
    val DATE_FORMAT
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun createLogFileName(): String {
        val formattedDate = FileUtils.createFileDate()
        return "key_mapper_log_$formattedDate.txt"
    }
}