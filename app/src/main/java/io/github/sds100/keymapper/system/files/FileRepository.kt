package io.github.sds100.keymapper.system.files

import android.content.Context
import io.github.sds100.keymapper.util.*
import java.io.File
import java.net.URL

/**
 * Created by sds100 on 04/04/2020.
 */
class FileRepository( context: Context) {

    private val ctx = context.applicationContext

    suspend fun getFile(url: String): Result<String> {
        val fileName = extractFileName(url)
        val path = FileUtils.getPathToFileInAppData(ctx, fileName)

        return Success("")
    }

    /**
     * Extracts the file name from the url
     */
    private fun extractFileName(fileUrl: String): String {
        return File(URL(fileUrl).path.toString()).name
    }
}