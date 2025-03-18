package io.github.sds100.keymapper.system.files

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.DecodeException
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.graphics.decodeBitmap
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar

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

    fun decodeBitmapFromUri(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return try {
                ImageDecoder.createSource(contentResolver, uri).decodeBitmap { _, _ -> }
            } catch (e: DecodeException) {
                null
            } catch (e: IOException) {
                null
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                return MediaStore.Images.Media.getBitmap(contentResolver, uri)
            } catch (_: FileNotFoundException) {
                // do nothing. The user just picked a file, how can it not exist?
                return null
            } catch (_: IOException) {
                return null
            }
        }
    }
}
