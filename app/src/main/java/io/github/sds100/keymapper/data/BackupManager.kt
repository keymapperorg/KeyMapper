package io.github.sds100.keymapper.data

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.repository.FingerprintMapRepository
import io.github.sds100.keymapper.data.usecase.BackupRestoreUseCase
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by sds100 on 23/01/21.
 */
class BackupManager(private val keymapRepository: BackupRestoreUseCase,
                    private val fingerprintMapRepository: FingerprintMapRepository,
                    private val deviceInfoRepository: DeviceInfoRepository,
                    private val coroutineScope: CoroutineScope,
                    private val contentResolver: IContentResolver,
                    private val globalPreferences: IGlobalPreferences,
                    private val throwExceptions: Boolean = false
) : IBackupManager {
    companion object {
        //DON'T CHANGE THESE. Used for serialization and parsing.
        @VisibleForTesting
        const val NAME_KEYMAP_DB_VERSION = "keymap_db_version"
        private const val NAME_KEYMAP_LIST = "keymap_list"
        private const val NAME_DEVICE_INFO = "device_info"
        private const val NAME_FINGERPRINT_SWIPE_DOWN = "fingerprint_swipe_down"
        private const val NAME_FINGERPRINT_SWIPE_UP = "fingerprint_swipe_up"
        private const val NAME_FINGERPRINT_SWIPE_LEFT = "fingerprint_swipe_left"
        private const val NAME_FINGERPRINT_SWIPE_RIGHT = "fingerprint_swipe_right"

        private val GESTURE_ID_TO_JSON_KEY_MAP = mapOf(
            FingerprintMapUtils.SWIPE_DOWN to NAME_FINGERPRINT_SWIPE_DOWN,
            FingerprintMapUtils.SWIPE_UP to NAME_FINGERPRINT_SWIPE_UP,
            FingerprintMapUtils.SWIPE_LEFT to NAME_FINGERPRINT_SWIPE_LEFT,
            FingerprintMapUtils.SWIPE_RIGHT to NAME_FINGERPRINT_SWIPE_RIGHT,
        )
    }

    private val _eventStream = LiveEvent<Event>()
    override val eventStream: LiveData<Event> = _eventStream

    private val gson = Gson()

    init {
        keymapRepository.requestBackup.observeForever {
            coroutineScope.launch {
                if (!shouldBackupAutomatically()) return@launch

                val uriString = globalPreferences.get(PreferenceKeys.automaticBackupLocation)
                    ?: return@launch

                val result =
                    contentResolver
                        .openOutputStream(uriString)
                        .suspendThen {
                            val keymaps = keymapRepository.getKeymaps()

                            backup(
                                it,
                                keymaps,
                                fingerprintMapRepository.swipeDown.firstOrNull(),
                                fingerprintMapRepository.swipeUp.firstOrNull(),
                                fingerprintMapRepository.swipeLeft.firstOrNull(),
                                fingerprintMapRepository.swipeRight.firstOrNull())
                        }

                _eventStream.value = AutomaticBackupResult(result)
            }
        }
    }

    override fun backupKeymaps(outputStream: OutputStream, keymapIds: List<Long>) {
        coroutineScope.launch {
            val keymaps = keymapRepository.getKeymaps().filter { keymapIds.contains(it.id) }

            backup(
                outputStream,
                keymaps
            )
        }
    }

    override fun backupFingerprintMaps(outputStream: OutputStream) {
        coroutineScope.launch {
            backup(
                outputStream,
                fingerprintSwipeDown = fingerprintMapRepository.swipeDown.firstOrNull(),
                fingerprintSwipeUp = fingerprintMapRepository.swipeUp.firstOrNull(),
                fingerprintSwipeLeft = fingerprintMapRepository.swipeLeft.firstOrNull(),
                fingerprintSwipeRight = fingerprintMapRepository.swipeRight.firstOrNull())
        }
    }

    override fun backupEverything(outputStream: OutputStream) {
        coroutineScope.launch {
            withContext(Dispatchers.Default) {
                val keymaps = keymapRepository.getKeymaps()

                backup(
                    outputStream,
                    keymaps,
                    fingerprintMapRepository.swipeDown.firstOrNull(),
                    fingerprintMapRepository.swipeUp.firstOrNull(),
                    fingerprintMapRepository.swipeLeft.firstOrNull(),
                    fingerprintMapRepository.swipeRight.firstOrNull())
            }
        }
    }

    override fun restore(inputStream: InputStream) {
        coroutineScope.launch {
            try {
                val json = inputStream.bufferedReader().use { it.readText() }
                val result = restore(json)
                _eventStream.value = RestoreResult(result)

            } catch (e: Exception) {
                _eventStream.value = RestoreResult(GenericFailure(e))

                if (throwExceptions) {
                    throw e
                }
            }
        }
    }

    private suspend fun restore(json: String): Result<Unit> {
        val parser = JsonParser()
        val gson = Gson()

        val rootElement = parser.parse(json).asJsonObject

        val keymapDbVersion = rootElement.get(NAME_KEYMAP_DB_VERSION).nullInt ?: 9

        val keymapListJsonArray by rootElement.byNullableArray(NAME_KEYMAP_LIST)

        val deviceInfoJsonArray by rootElement.byNullableArray(NAME_DEVICE_INFO)
        val deviceInfoList = gson.fromJson<List<DeviceInfo>>(deviceInfoJsonArray ?: JsonArray())

        //started storing database version at db version 10
        if (keymapDbVersion > AppDatabase.DATABASE_VERSION) {
            return IncompatibleBackup()
        }

        keymapListJsonArray
            ?.toList()
            ?.map { gson.toJson(it) }
            ?.let {
                keymapRepository.restore(keymapDbVersion, it)
            }

        deviceInfoRepository.insertDeviceInfo(*deviceInfoList.toTypedArray())

        FingerprintMapUtils.GESTURES.forEach { gestureId ->
            val element = rootElement.get(GESTURE_ID_TO_JSON_KEY_MAP[gestureId])

            element ?: return@forEach

            val version = element.asJsonObject.get(FingerprintMap.NAME_VERSION).nullInt ?: 0
            val incompatible = version > FingerprintMap.CURRENT_VERSION

            if (incompatible) {
                return IncompatibleBackup()
            }

            fingerprintMapRepository.restore(gestureId, gson.toJson(element))
        }

        return Success(Unit)
    }

    private suspend fun backup(
        outputStream: OutputStream,
        keymapList: List<KeyMap>? = null,
        fingerprintSwipeDown: FingerprintMap? = null,
        fingerprintSwipeUp: FingerprintMap? = null,
        fingerprintSwipeLeft: FingerprintMap? = null,
        fingerprintSwipeRight: FingerprintMap? = null
    ): Result<Unit> = coroutineScope.async(Dispatchers.IO) {
        try {
            val allDeviceInfoList = deviceInfoRepository.getAll()

            //delete the contents of the file
            if (outputStream is FileOutputStream) {
                outputStream.channel.truncate(0)
            }

            val deviceInfoIdsToBackup = mutableSetOf<String>()

            keymapList?.forEach { keymap ->
                keymap.trigger.keys.forEach { key ->
                    if (key.deviceId != Trigger.Key.DEVICE_ID_ANY_DEVICE
                        && key.deviceId != Trigger.Key.DEVICE_ID_THIS_DEVICE) {
                        deviceInfoIdsToBackup.add(key.deviceId)
                    }
                }
            }

            val deviceInfoList = deviceInfoIdsToBackup
                .map { id -> allDeviceInfoList.single { it.descriptor == id } }
                .takeIf { it.isNotEmpty() }

            val keymapDbVersion = keymapList?.let { AppDatabase.DATABASE_VERSION }

            outputStream.bufferedWriter().use { writer ->
                val json = gson.toJson(
                    BackupModel(
                        keymapDbVersion,
                        keymapList,
                        deviceInfoList,
                        fingerprintSwipeDown,
                        fingerprintSwipeUp,
                        fingerprintSwipeLeft,
                        fingerprintSwipeRight
                    ))

                writer.write(json)

                return@async Success(Unit)
            }
        } catch (e: Exception) {
            if (throwExceptions) throw e

            return@async GenericFailure(e)
        }
    }.await()

    private suspend fun shouldBackupAutomatically() =
        globalPreferences.get(PreferenceKeys.automaticBackupLocation)?.isNotBlank() ?: false

    private class BackupModel(
        @SerializedName(NAME_KEYMAP_DB_VERSION)
        val keymapDbVersion: Int? = null,

        @SerializedName(NAME_KEYMAP_LIST)
        val keymapList: List<KeyMap>? = null,

        @SerializedName(NAME_DEVICE_INFO)
        val deviceInfo: List<DeviceInfo>? = null,

        @SerializedName(NAME_FINGERPRINT_SWIPE_DOWN)
        val fingerprintSwipeDown: FingerprintMap?,

        @SerializedName(NAME_FINGERPRINT_SWIPE_UP)
        val fingerprintSwipeUp: FingerprintMap?,

        @SerializedName(NAME_FINGERPRINT_SWIPE_LEFT)
        val fingerprintSwipeLeft: FingerprintMap?,

        @SerializedName(NAME_FINGERPRINT_SWIPE_RIGHT)
        val fingerprintSwipeRight: FingerprintMap?
    )
}