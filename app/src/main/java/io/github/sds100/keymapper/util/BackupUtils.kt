package io.github.sds100.keymapper.util

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.result.FileAccessDenied
import io.github.sds100.keymapper.util.result.GenericFailure
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by sds100 on 17/06/2020.
 */
object BackupUtils {

    const val DEFAULT_AUTOMATIC_BACKUP_NAME = "keymapper_keymaps.json"

    //DON'T CHANGE THESE. Used for serialization and parsing.
    private const val NAME_KEYMAP_LIST = "keymap_list"
    private const val NAME_DEVICE_INFO = "device_info"

    val backupAutomatically: Boolean
        get() = AppPreferences.automaticBackupLocation.isNotBlank()

    suspend fun backup(outputStream: OutputStream,
                       keymapList: List<KeyMap>,
                       allDeviceInfo: List<DeviceInfo>): Result<Unit> = withContext(Dispatchers.IO) {

        try {
            //delete the contents of the file
            (outputStream as FileOutputStream).channel.truncate(0)
            val deviceInfoIdsToBackup = mutableSetOf<String>()

            keymapList.forEach { keymap ->
                keymap.id = 0

                keymap.trigger.keys.forEach { key ->
                    if (key.deviceId != Trigger.Key.DEVICE_ID_ANY_DEVICE
                        && key.deviceId != Trigger.Key.DEVICE_ID_THIS_DEVICE) {
                        deviceInfoIdsToBackup.add(key.deviceId)
                    }
                }
            }

            val deviceInfoList = deviceInfoIdsToBackup.map { id -> allDeviceInfo.single { it.descriptor == id } }

            outputStream.bufferedWriter().use { bufferedWriter ->
                val json = Gson().toJson(BackupModel(keymapList, deviceInfoList))

                bufferedWriter.write(json)
            }
        } catch (e: Exception) {
            return@withContext GenericFailure(e)
        }

        return@withContext Success(Unit)
    }

    suspend fun restore(inputStream: InputStream): Result<RestoreModel> = withContext(Dispatchers.IO) {
        try {
            inputStream.bufferedReader().use { bufferedReader ->

                val json = bufferedReader.readText()
                val parser = JsonParser()

                val gson = GsonBuilder().registerTypeAdapter<KeyMap> {

                    deserialize {

                        val actionListJsonArray by it.json.byArray(KeyMap.NAME_ACTION_LIST)
                        val actionList = it.context.deserialize<List<Action>>(actionListJsonArray)

                        val triggerJsonObject by it.json.byObject(KeyMap.NAME_TRIGGER)
                        val trigger = it.context.deserialize<Trigger>(triggerJsonObject)

                        val constraintListJsonArray by it.json.byArray(KeyMap.NAME_CONSTRAINT_LIST)
                        val constraintList = it.context.deserialize<List<Constraint>>(constraintListJsonArray)

                        val constraintMode by it.json.byInt(KeyMap.NAME_CONSTRAINT_MODE)
                        val flags by it.json.byInt(KeyMap.NAME_FLAGS)
                        val folderName by it.json.byNullableString(KeyMap.NAME_FOLDER_NAME)
                        val isEnabled by it.json.byBool(KeyMap.NAME_IS_ENABLED)

                        KeyMap(
                            0,
                            trigger,
                            actionList,
                            constraintList,
                            constraintMode,
                            flags,
                            folderName,
                            isEnabled
                        )
                    }
                }.registerTypeAdapter<Trigger.Key> {

                    deserialize {
                        val keycode by it.json.byInt(Trigger.Key.NAME_KEYCODE)
                        val deviceId by it.json.byString(Trigger.Key.NAME_DEVICE_ID)
                        val clickType by it.json.byInt(Trigger.Key.NAME_CLICK_TYPE)

                        Trigger.Key(keycode, deviceId, clickType)
                    }
                }.registerTypeAdapter<Trigger> {

                    deserialize {
                        val triggerKeysJsonArray by it.json.byArray(Trigger.NAME_KEYS)
                        val keys = it.context.deserialize<List<Trigger.Key>>(triggerKeysJsonArray)

                        val extrasJsonArray by it.json.byArray(Trigger.NAME_EXTRAS)
                        val extraList = it.context.deserialize<List<Extra>>(extrasJsonArray) ?: listOf()

                        val mode by it.json.byInt(Trigger.NAME_MODE)

                        val flags by it.json.byNullableInt(Trigger.NAME_FLAGS)

                        Trigger(keys, extraList, mode, flags ?: 0)
                    }
                }.registerTypeAdapter<Action> {

                    deserialize {
                        val typeString by it.json.byString(Action.NAME_ACTION_TYPE)
                        val type = ActionType.valueOf(typeString)

                        val data by it.json.byString(Action.NAME_DATA)

                        val extrasJsonArray by it.json.byArray(Action.NAME_EXTRAS)
                        val extraList = it.context.deserialize<List<Extra>>(extrasJsonArray) ?: listOf()

                        val flags by it.json.byInt(Action.NAME_FLAGS)

                        Action(type, data, extraList.toMutableList(), flags)
                    }

                }.registerTypeAdapter<Constraint> {

                    deserialize {
                        val type by it.json.byString(Constraint.NAME_TYPE)

                        val extrasJsonArray by it.json.byArray(Constraint.NAME_EXTRAS)
                        val extraList = it.context.deserialize<List<Extra>>(extrasJsonArray) ?: listOf()

                        Constraint(type, extraList)
                    }
                }.registerTypeAdapter<DeviceInfo> {

                    deserialize {
                        val descriptor by it.json.byString(DeviceInfo.NAME_DESCRIPTOR)
                        val name by it.json.byString(DeviceInfo.NAME_NAME)

                        DeviceInfo(descriptor, name)
                    }
                }.registerTypeAdapter<Extra> {

                    deserialize {
                        val id by it.json.byString(Extra.NAME_ID)
                        val data by it.json.byString(Extra.NAME_DATA)

                        Extra(id, data)
                    }
                }.create()

                val rootElement = parser.parse(json)
                val keymapListJsonArray by rootElement.byArray(NAME_KEYMAP_LIST)
                val keymapList = gson.fromJson<List<KeyMap>>(keymapListJsonArray)

                val deviceInfoJsonArray by rootElement.byArray(NAME_DEVICE_INFO)
                val deviceInfoList = gson.fromJson<List<DeviceInfo>>(deviceInfoJsonArray)

                return@withContext Success(RestoreModel(keymapList, deviceInfoList))
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

    class RestoreModel(val keymapList: List<KeyMap>, val deviceInfo: List<DeviceInfo>)

    private class BackupModel(@SerializedName(NAME_KEYMAP_LIST)
                              val keymapList: List<KeyMap>,

                              @SerializedName(NAME_DEVICE_INFO)
                              val deviceInfo: List<DeviceInfo>)
}