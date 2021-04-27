package io.github.sds100.keymapper.system.files

import android.annotation.SuppressLint
import android.content.Context
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by sds100 on 15/12/2018.
 */

object FileUtils {

    const val MIME_TYPE_IMAGES = "image/*"
    const val MIME_TYPE_PNG = "image/png"
    const val MIME_TYPE_ALL = "*/*"

    fun getTextFroappFiles(ctx: Context, fileName: String): String {
        ctx.openFileInput(fileName).bufferedReader().use {
            return it.readText()
        }
    }

    fun appendTextToFile(path: String, vararg lines: String) {
        FileOutputStream(path, true).bufferedWriter().use { writer ->
            lines.forEach { line ->
                writer.append(line)
            }
        }
    }

    fun getPathToFileInAppData(ctx: Context, fileName: String) = "${ctx.filesDir.path}/$fileName"

    @SuppressLint("SimpleDateFormat")
    fun createFileDate(): String {
        val date = Calendar.getInstance().time
        val format = SimpleDateFormat("yyyyMMdd-HHmmss")

        return format.format(date)
    }
}