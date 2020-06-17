package io.github.sds100.keymapper.util

import android.annotation.SuppressLint
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.util.result.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by sds100 on 17/06/2020.
 */
object BackupUtils {

    suspend fun backup(outputStream: OutputStream,
                       keymapList: List<KeyMap>): Result<Unit> = withContext(Dispatchers.IO) {

        try {
            keymapList.forEach {
                it.id = 0
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
                return@withContext Success(backupModel.keymapList)
            }

        } catch (e: Exception) {
            return@withContext GenericFailure(e)
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun createFileName(): String {
        val date = Calendar.getInstance().time
        val format = SimpleDateFormat("yyyyMMdd_HHmmss")

        val formattedDate = format.format(date)
        return "keymaps_$formattedDate.json"
    }

    //DON'T CHANGE THE PROPERTY NAMES
    private class BackupModel(val dbVersion: Int, val keymapList: List<KeyMap>)
}