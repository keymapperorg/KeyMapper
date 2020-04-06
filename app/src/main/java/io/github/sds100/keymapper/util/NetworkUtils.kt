package io.github.sds100.keymapper.util

import android.util.Log
import com.android.volley.NoConnectionError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import io.github.sds100.keymapper.util.result.DownloadFailed
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.SSLHandshakeError
import io.github.sds100.keymapper.util.result.Success
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.init.appCtx
import java.io.File
import javax.net.ssl.SSLHandshakeException
import kotlin.coroutines.resume

/**
 * Created by sds100 on 04/04/2020.
 */
object NetworkUtils {

    @ExperimentalCoroutinesApi
    suspend fun downloadFile(
        url: String,
        filePath: String
    ): Result<File> = suspendCancellableCoroutine {

        val queue = Volley.newRequestQueue(appCtx)

        val request = StringRequest(Request.Method.GET, url,
            Response.Listener { response ->
                val file = File(filePath)
                file.writeText(response)

                it.resume(Success(file))
            },

            Response.ErrorListener { error ->
                if (error.cause is SSLHandshakeException) {
                    it.resume(SSLHandshakeError())
                } else {
                    it.resume(DownloadFailed())
                }
            })

        it.invokeOnCancellation {
            request.cancel()
        }

        queue.add(request)
    }
}