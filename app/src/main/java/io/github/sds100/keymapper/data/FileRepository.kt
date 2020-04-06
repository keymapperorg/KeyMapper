package io.github.sds100.keymapper.data

import android.content.Context
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.FileUtils
import io.github.sds100.keymapper.util.NetworkUtils
import io.github.sds100.keymapper.util.result.*
import splitties.resources.appStr
import java.io.File
import java.net.URL

/**
 * Created by sds100 on 04/04/2020.
 */
class FileRepository private constructor(private val mContext: Context) {
    companion object {
        @Volatile
        private var instance: FileRepository? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: FileRepository(context.applicationContext).also { instance = it }
            }
    }

    suspend fun getFile(url: String): Result<String>{
        val fileName = extractFileName(url)
        val path = FileUtils.getPathToFileInAppData(mContext, fileName)

        return NetworkUtils.downloadFile(url, path)
            .otherwise {
                val file = File(path)

                if (file.exists() && file.readText().isNotBlank()) {
                    Success(file)
                } else {
                    FileNotExists()
                }
            }
            .then { Success(it.readText()) }
    }

    /**
     * Extracts the file name from the url
     */
    private fun extractFileName(fileUrl: String): String {
        return File(URL(fileUrl).path.toString()).name
    }
}