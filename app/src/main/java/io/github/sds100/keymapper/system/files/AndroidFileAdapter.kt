package io.github.sds100.keymapper.system.files

import android.content.Context
import android.net.Uri
import android.os.Environment
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by sds100 on 13/04/2021.
 */
class AndroidFileAdapter(context: Context) : FileAdapter {
    private val ctx = context.applicationContext

    override fun openOutputStream(uriString: String): Result<OutputStream> {
        val uri = Uri.parse(uriString)

        return try {
            val outputStream = ctx.contentResolver.openOutputStream(uri)!!

            Success(outputStream)
        } catch (e: Exception) {
            when (e) {
                is SecurityException -> Error.FileAccessDenied
                else -> Error.Exception(e)
            }
        }
    }

    override fun openInputStream(uriString: String): Result<InputStream> {
        val uri = Uri.parse(uriString)

        return try {
            val inputStream = ctx.contentResolver.openInputStream(uri)!!

            Success(inputStream)
        } catch (e: Exception) {
            when (e) {
                is SecurityException -> Error.FileAccessDenied
                else -> Error.Exception(e)
            }
        }
    }

    override fun getPicturesFolder(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    }
}