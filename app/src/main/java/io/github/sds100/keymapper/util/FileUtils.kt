package io.github.sds100.keymapper.util

import android.content.Context
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.io.FileOutputStream

/**
 * Created by sds100 on 15/12/2018.
 */

object FileUtils {

    const val MIME_TYPE_JSON = "application/json"

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

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun readFileContents(activity: AppCompatActivity,
                         mimeType: String,
                         onOpen: (contents: String) -> Unit) {

        activity.registerForActivityResult(ActivityResultContracts.GetContent()) {
            it ?: return@registerForActivityResult

            val inputStream = activity.contentResolver.openInputStream(it)

            val contents = inputStream?.bufferedReader().use { reader -> reader?.readText() }
                ?: return@registerForActivityResult

            onOpen(contents)
        }.launch(mimeType)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun writeFileContents(activity: AppCompatActivity, fileName: String, contents: String) {
        activity.registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            it ?: return@registerForActivityResult

            val outputStream = activity.contentResolver.openOutputStream(it)

            outputStream?.bufferedWriter().use { writer ->
                writer?.write(contents)
            }
        }.launch(fileName)
    }
}