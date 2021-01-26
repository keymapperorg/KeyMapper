package io.github.sds100.keymapper.util

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.result.FileAccessDenied
import io.github.sds100.keymapper.util.result.GenericFailure
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by sds100 on 17/06/2020.
 */
object BackupUtils {

    const val DEFAULT_AUTOMATIC_BACKUP_NAME = "keymapper_keymaps.json"

    fun createFileName(): String {
        val formattedDate = FileUtils.createFileDate()
        return "keymaps_$formattedDate.json"
    }
}