package io.github.sds100.keymapper.backup

import androidx.annotation.VisibleForTesting
import com.github.salomonbrys.kotson.byNullableArray
import com.github.salomonbrys.kotson.byNullableObject
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.nullInt
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.MalformedJsonException
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.system.devices.DeviceInfoEntity
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapEntity
import io.github.sds100.keymapper.mappings.keymaps.KeyMapEntity
import io.github.sds100.keymapper.data.entities.TriggerEntity
import io.github.sds100.keymapper.system.devices.DeviceInfoCache
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapRepository
import io.github.sds100.keymapper.mappings.keymaps.KeyMapRepository
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapEntityGroup
import io.github.sds100.keymapper.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.util.DispatcherProvider
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.suspendThen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Created by sds100 on 16/03/2021.
 */

class BackupManagerImpl(
    private val coroutineScope: CoroutineScope,
    private val fileAdapter: FileAdapter,
    private val keyMapRepository: KeyMapRepository,
    private val deviceInfoRepository: DeviceInfoCache,
    private val preferenceRepository: PreferenceRepository,
    private val fingerprintMapRepository: FingerprintMapRepository,
    private val throwExceptions: Boolean = false,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BackupManager {

    companion object {
        //DON'T CHANGE THESE. Used for serialization and parsing.
        @VisibleForTesting
        const val NAME_KEYMAP_DB_VERSION = "keymap_db_version"

        private const val NAME_KEYMAP_LIST = "keymap_list"

        //TODO deprecate
        private const val NAME_DEVICE_INFO = "device_info"

        private const val NAME_FINGERPRINT_SWIPE_DOWN = "fingerprint_swipe_down"
        private const val NAME_FINGERPRINT_SWIPE_UP = "fingerprint_swipe_up"
        private const val NAME_FINGERPRINT_SWIPE_LEFT = "fingerprint_swipe_left"
        private const val NAME_FINGERPRINT_SWIPE_RIGHT = "fingerprint_swipe_right"

        private val GESTURE_ID_TO_JSON_KEY_MAP = mapOf(
            FingerprintMapEntity.ID_SWIPE_DOWN to NAME_FINGERPRINT_SWIPE_DOWN,
            FingerprintMapEntity.ID_SWIPE_UP to NAME_FINGERPRINT_SWIPE_UP,
            FingerprintMapEntity.ID_SWIPE_LEFT to NAME_FINGERPRINT_SWIPE_LEFT,
            FingerprintMapEntity.ID_SWIPE_RIGHT to NAME_FINGERPRINT_SWIPE_RIGHT,
        )
    }

    override val onBackupResult = MutableSharedFlow<Result<*>>()
    override val onRestoreResult = MutableSharedFlow<Result<*>>()
    override val onAutomaticBackupResult = MutableSharedFlow<Result<*>>()

    private val gson = Gson()

    private val backupAutomatically: Flow<Boolean> = preferenceRepository
        .get(Keys.automaticBackupLocation).map { it != null }

    init {
        val doAutomaticBackup = MutableSharedFlow<BackupData>()

        coroutineScope.launch {
            doAutomaticBackup.collectLatest { backupData ->
                if (!backupAutomatically.first()) return@collectLatest

                val backupLocation = preferenceRepository.get(Keys.automaticBackupLocation).first()
                    ?: return@collectLatest

                val result = fileAdapter
                    .openOutputStream(backupLocation)
                    .suspendThen { outputStream ->
                        backupAsync(outputStream, backupData).await()
                    }

                onAutomaticBackupResult.emit(result)
            }
        }

        coroutineScope.launch {
            keyMapRepository.requestBackup.collectLatest { keyMapList ->

                doAutomaticBackup.emit(
                    BackupData(
                        keyMapList = keyMapList,
                        fingerprintMaps = fingerprintMapRepository.fingerprintMaps.firstOrNull()
                    )
                )
            }
        }

        coroutineScope.launch {
            fingerprintMapRepository.requestBackup.collectLatest { fingerprintMaps ->
                doAutomaticBackup.emit(
                    BackupData(
                        keyMapList = keyMapRepository.keyMapList.firstOrNull()?.dataOrNull(),
                        fingerprintMaps = fingerprintMaps
                    )
                )
            }
        }

        //automatically back up when the location changes
        preferenceRepository.get(Keys.automaticBackupLocation).drop(1).onEach {
            val data = BackupData(
                keyMapList = keyMapRepository.keyMapList.firstOrNull()?.dataOrNull(),
                fingerprintMaps = fingerprintMapRepository.fingerprintMaps.firstOrNull()
            )

            doAutomaticBackup.emit(data)
        }.launchIn(coroutineScope)
    }

    override fun backupKeyMaps(uri: String, keyMapIds: List<String>) {
        coroutineScope.launch(dispatchers.default()) {
            val result = fileAdapter
                .openOutputStream(uri)
                .suspendThen { outputStream ->
                    val allKeyMaps = keyMapRepository.keyMapList
                        .first { it is State.Data } as State.Data

                    val keyMapsToBackup = allKeyMaps.data.filter { keyMapIds.contains(it.uid) }

                    val data = BackupData(
                        keyMapList = keyMapsToBackup
                    )

                    backupAsync(outputStream, data).await()
                }

            onBackupResult.emit(result)
        }
    }

    override fun backupFingerprintMaps(uri: String) {
        coroutineScope.launch(dispatchers.default()) {
            val result = fileAdapter
                .openOutputStream(uri)
                .suspendThen { outputStream ->
                    val data = BackupData(
                        fingerprintMaps = fingerprintMapRepository.fingerprintMaps.firstOrNull()
                    )

                    backupAsync(outputStream, data).await()
                }


            onBackupResult.emit(result)
        }
    }

    override fun backupMappings(uri: String) {
        coroutineScope.launch(dispatchers.default()) {
            val result = fileAdapter
                .openOutputStream(uri)
                .suspendThen { outputStream ->
                    val data = BackupData(
                        keyMapList = keyMapRepository.keyMapList.first().dataOrNull(),
                        fingerprintMaps = fingerprintMapRepository.fingerprintMaps.firstOrNull()
                    )

                    backupAsync(outputStream, data).await()
                }

            onBackupResult.emit(result)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override fun restoreMappings(uri: String) {
        coroutineScope.launch(dispatchers.default()) {
            val result = fileAdapter
                .openInputStream(uri)
                .suspendThen { inputStream ->
                    val json = inputStream.bufferedReader().use { it.readText() }
                    restore(json)
                }

            onRestoreResult.emit(result)
        }
    }

    private suspend fun restore(json: String): Result<*> {
        try {
            val parser = JsonParser()
            val gson = Gson()

            if (json.isBlank()) return Error.EmptyJson

            val rootElement = parser.parse(json).asJsonObject

            val keymapDbVersion = rootElement.get(NAME_KEYMAP_DB_VERSION).nullInt ?: 9

            val keymapListJsonArray by rootElement.byNullableArray(NAME_KEYMAP_LIST)
            val deviceInfoJsonArray by rootElement.byNullableArray(NAME_DEVICE_INFO)

            val deviceInfoList =
                gson.fromJson<List<DeviceInfoEntity>>(deviceInfoJsonArray ?: JsonArray())

            //started storing database version at db version 10
            if (keymapDbVersion > AppDatabase.DATABASE_VERSION) {
                return Error.BackupVersionTooNew
            }

            keymapListJsonArray
                ?.toList()
                ?.map { gson.toJson(it) }
                ?.let {
                    keyMapRepository.restore(keymapDbVersion, it)
                }

            deviceInfoRepository.insertDeviceInfo(*deviceInfoList.toTypedArray())

            FingerprintMapEntity.GESTURES.forEach { gestureId ->
                val element by rootElement.byNullableObject(GESTURE_ID_TO_JSON_KEY_MAP[gestureId])

                element ?: return@forEach

                val version = element!!.get(FingerprintMapEntity.NAME_VERSION).nullInt ?: 0
                val incompatible = version > FingerprintMapEntity.CURRENT_VERSION

                if (incompatible) {
                    return Error.BackupVersionTooNew
                }

                fingerprintMapRepository.restore(gestureId, gson.toJson(element))
            }

            return Success(Unit)

        } catch (e: MalformedJsonException) {
            return Error.CorruptJsonFile(e.message ?: "")

        } catch (e: JsonSyntaxException) {
            return Error.CorruptJsonFile(e.message ?: "")

        } catch (e: NoSuchElementException) {
            return Error.CorruptJsonFile(e.message ?: "")

        } catch (e: Exception) {

            if (throwExceptions) {
                e.printStackTrace()
                throw e
            }

            return Error.Exception(e)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun backupAsync(
        outputStream: OutputStream,
        data: BackupData
    ) = coroutineScope.async(dispatchers.io()) {
        try {
            val allDeviceInfoList = deviceInfoRepository.getAll()

            //delete the contents of the file
            if (outputStream is FileOutputStream) {
                outputStream.channel.truncate(0)
            }

            val deviceInfoIdsToBackup = mutableSetOf<String>()

            data.keyMapList?.forEach { keymap ->
                keymap.trigger.keys.forEach { key ->
                    if (key.deviceId != TriggerEntity.KeyEntity.DEVICE_ID_ANY_DEVICE
                        && key.deviceId != TriggerEntity.KeyEntity.DEVICE_ID_THIS_DEVICE
                    ) {
                        deviceInfoIdsToBackup.add(key.deviceId)
                    }
                }
            }

            val deviceInfoList = deviceInfoIdsToBackup
                .map { id -> allDeviceInfoList.single { it.descriptor == id } }
                .takeIf { it.isNotEmpty() }

            val keyMapDbVersion = data.keyMapList?.let { AppDatabase.DATABASE_VERSION }

            outputStream.bufferedWriter().use { writer ->

                val json = gson.toJson(
                    BackupModel(
                        keyMapDbVersion,
                        data.keyMapList,
                        deviceInfoList,
                        data.fingerprintMaps?.swipeDown,
                        data.fingerprintMaps?.swipeUp,
                        data.fingerprintMaps?.swipeLeft,
                        data.fingerprintMaps?.swipeRight,
                    )
                )

                writer.write(json)
            }

            return@async Success(Unit)
        } catch (e: Exception) {
            if (throwExceptions) throw e

            return@async Error.Exception(e)
        }
    }

    private data class BackupData(
        val keyMapList: List<KeyMapEntity>? = null,
        val fingerprintMaps: FingerprintMapEntityGroup? = null
    )

    //TODO eventually delete and have a custom serializer for backup data
    private data class BackupModel(
        @SerializedName(NAME_KEYMAP_DB_VERSION)
        val keymapDbVersion: Int? = null,

        @SerializedName(NAME_KEYMAP_LIST)
        val keymapList: List<KeyMapEntity>? = null,

        @SerializedName(NAME_DEVICE_INFO)
        val deviceInfo: List<DeviceInfoEntity>? = null,

        @SerializedName(NAME_FINGERPRINT_SWIPE_DOWN)
        val fingerprintSwipeDown: FingerprintMapEntity?,

        @SerializedName(NAME_FINGERPRINT_SWIPE_UP)
        val fingerprintSwipeUp: FingerprintMapEntity?,

        @SerializedName(NAME_FINGERPRINT_SWIPE_LEFT)
        val fingerprintSwipeLeft: FingerprintMapEntity?,

        @SerializedName(NAME_FINGERPRINT_SWIPE_RIGHT)
        val fingerprintSwipeRight: FingerprintMapEntity?
    )
}

interface BackupManager {
    val onBackupResult: Flow<Result<*>>
    val onRestoreResult: Flow<Result<*>>
    val onAutomaticBackupResult: Flow<Result<*>>

    fun backupKeyMaps(uri: String, keyMapIds: List<String>)
    fun backupFingerprintMaps(uri: String)
    fun backupMappings(uri: String)
    fun restoreMappings(uri: String)
}