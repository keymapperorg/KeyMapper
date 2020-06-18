package io.github.sds100.keymapper.util

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.util.result.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.StringReader

/**
 * Created by sds100 on 17/06/2020.
 */
object BackupUtils {

    const val DEFAULT_AUTOMATIC_BACKUP_NAME = "keymapper_keymaps.json"

    val backupAutomatically: Boolean
        get() = AppPreferences.automaticBackupLocation.isNotBlank()

    suspend fun backup(outputStream: OutputStream,
                       keymapList: List<KeyMap>): Result<Unit> = withContext(Dispatchers.IO) {

        try {
            //delete the contents of the file
            (outputStream as FileOutputStream).channel.truncate(0)

            keymapList.forEach { keymap ->
                keymap.id = 0
                keymap.trigger.keys.forEach {
                    it.deviceId = Trigger.Key.DEVICE_ID_ANY_DEVICE
                }
            }

            outputStream.bufferedWriter().use { bufferedWriter ->
                val json = Gson().toJson(BackupModel(AppDatabase.DATABASE_VERSION, keymapList))

                bufferedWriter.write(json)
            }
        } catch (e: Exception) {
            return@withContext GenericFailure(e)
        }

        return@withContext Success(Unit)
    }

    suspend fun restore(inputStream: InputStream): Result<List<KeyMap>> = withContext(Dispatchers.IO) {
        try {
            inputStream.bufferedReader().use { bufferedReader ->

                val json = bufferedReader.readText()

                val gson = Gson()

                gson.newJsonReader(StringReader(json)).use {
                    it.beginObject()
                    it.nextName()
                    val dbVersion = it.nextInt()

                    if (dbVersion > AppDatabase.DATABASE_VERSION) {
                        return@withContext ClientVersionTooOld()
                    } else if (dbVersion < AppDatabase.DATABASE_VERSION) {
                        return@withContext BackupVersionTooOld()
                    }
                }

                val backupModel = gson.fromJson<BackupModel>(json)

                backupModel.keymapList.forEach { keymap ->
                    keymap.trigger.keys.forEach {
                        it.deviceId = Trigger.Key.DEVICE_ID_ANY_DEVICE
                    }
                }

                return@withContext Success(backupModel.keymapList)
            }

        } catch (e: Exception) {
            return@withContext GenericFailure(e)
        }
    }

    fun createFileName(): String {
        val formattedDate = FileUtils.createFileDate()
        return "keymaps_$formattedDate.json"
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getAutomaticBackupLocation(context: Context): Result<String> {
        val uri = Uri.parse(AppPreferences.automaticBackupLocation)

        return Success(uri.path!!)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun createAutomaticBackupOutputStream(context: Context): Result<OutputStream> {
        val uri = Uri.parse(AppPreferences.automaticBackupLocation)
        val contentResolver = context.contentResolver

        return try {
            val outputStream = contentResolver.openOutputStream(uri)!!

            Success(outputStream)
        } catch (e: Exception) {
            when (e) {
                is SecurityException -> FileAccessDenied()
                else -> GenericFailure(e)
            }
        }
    }

    //DON'T CHANGE THE PROPERTY NAMES
    private class BackupModel(val dbVersion: Int, val keymapList: List<KeyMap>)
}