package io.github.sds100.keymapper.Utils

import android.content.Context

/**
 * Created by sds100 on 15/12/2018.
 */

object FileUtils {

    fun getTextFromFile(ctx: Context, path: String): String {
        ctx.openFileInput(path).bufferedReader().use {
            return it.readText()
        }
    }

    fun getPathToFileInAppData(ctx: Context, fileName: String) = "${ctx.filesDir.path}/$fileName"
}