package io.github.sds100.keymapper.system.files

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by sds100 on 15/12/2018.
 */

object FileUtils {

    const val MIME_TYPE_IMAGES = "image/*"
    const val MIME_TYPE_PNG = "image/png"
    const val MIME_TYPE_ALL = "*/*"
    const val MIME_TYPE_AUDIO = "audio/*"
    const val MIME_TYPE_ZIP = "application/zip"

    @SuppressLint("SimpleDateFormat")
    fun createFileDate(): String {
        val date = Calendar.getInstance().time
        val format = SimpleDateFormat("yyyyMMdd-HHmmss")

        return format.format(date)
    }
}