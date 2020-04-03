package io.github.sds100.keymapper.util

import android.content.Context
import java.io.FileOutputStream

/**
 * Created by sds100 on 15/12/2018.
 */

object FileUtils {

    fun getTextFromAppFiles(ctx: Context, fileName: String): String {
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
}