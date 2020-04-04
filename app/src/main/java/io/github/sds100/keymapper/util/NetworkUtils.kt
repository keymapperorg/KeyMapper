package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.util.result.DownloadFailed
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import java.io.File

/**
 * Created by sds100 on 04/04/2020.
 */
object NetworkUtils {

    suspend fun downloadFile(url: String, filePath: String): Result<File> = withContext(Dispatchers.IO) {
        val httpClient = OkHttpClient()
        val request = Request.Builder().url(url).build()

        return@withContext try {
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful && response.body != null) {
                val text = response.body!!.string()
                val file = File(filePath)
                file.writeText(text)

                Success(file)

            } else {
                DownloadFailed()
            }
        } catch (e: IOException) {
            DownloadFailed()
        }
    }
}