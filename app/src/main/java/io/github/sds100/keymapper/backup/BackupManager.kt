package io.github.sds100.keymapper.backup

import androidx.annotation.VisibleForTesting
import com.github.salomonbrys.kotson.byInt
import com.github.salomonbrys.kotson.byNullableArray
import com.github.salomonbrys.kotson.byNullableInt
import com.github.salomonbrys.kotson.byNullableObject
import com.github.salomonbrys.kotson.contains
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.nullInt
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.MalformedJsonException
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.actions.sound.SoundsManager
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.entities.FingerprintMapEntity
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.data.migration.JsonMigration
import io.github.sds100.keymapper.data.migration.Migration10To11
import io.github.sds100.keymapper.data.migration.Migration11To12
import io.github.sds100.keymapper.data.migration.Migration9To10
import io.github.sds100.keymapper.data.migration.MigrationUtils
import io.github.sds100.keymapper.data.migration.fingerprintmaps.FingerprintMapMigration0To1
import io.github.sds100.keymapper.data.migration.fingerprintmaps.FingerprintMapMigration1To2
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapRepository
import io.github.sds100.keymapper.mappings.keymaps.KeyMapRepository
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.IFile
import io.github.sds100.keymapper.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.util.DefaultUuidGenerator
import io.github.sds100.keymapper.util.DispatcherProvider
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.UuidGenerator
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.then
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream

/**
 * Created by sds100 on 16/03/2021.
 */

class BackupManagerImpl(
    private val coroutineScope: CoroutineScope,
    private val fileAdapter: FileAdapter,
    private val keyMapRepository: KeyMapRepository,
    private val preferenceRepository: PreferenceRepository,
    private val fingerprintMapRepository: FingerprintMapRepository,
    private val soundsManager: SoundsManager,
    private val throwExceptions: Boolean = false,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    private val uuidGenerator: UuidGenerator = DefaultUuidGenerator(),
) : BackupManager {

    companion object {
        // DON'T CHANGE THESE. Used for serialization and parsing.
        @VisibleForTesting
        private const val NAME_DB_VERSION = "keymap_db_version"
        private const val NAME_APP_VERSION = "app_version"
        private const val NAME_KEYMAP_LIST = "keymap_list"
        private const val NAME_FINGERPRINT_MAP_LIST = "fingerprint_map_list"
        private const val NAME_DEFAULT_LONG_PRESS_DELAY = "default_long_press_delay"
        private const val NAME_DEFAULT_DOUBLE_PRESS_DELAY = "default_double_press_delay"
        private const val NAME_DEFAULT_VIBRATION_DURATION = "default_vibration_duration"
        private const val NAME_DEFAULT_REPEAT_DELAY = "default_repeat_delay"
        private const val NAME_DEFAULT_REPEAT_RATE = "default_repeat_rate"
        private const val NAME_DEFAULT_SEQUENCE_TRIGGER_TIMEOUT = "default_sequence_trigger_timeout"

        // DON'T CHANGE THIS.
        private const val DATA_JSON_FILE_NAME = "data.json"
        private const val SOUNDS_DIR_NAME = "sounds"

        private const val TEMP_BACKUP_ROOT_DIR = "backup_temp"
        private const val TEMP_RESTORE_ROOT_DIR = "restore_temp"
    }

    override val onAutomaticBackupResult = MutableSharedFlow<Result<*>>()

    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(FingerprintMapEntity.DESERIALIZER)
            .registerTypeAdapter(KeyMapEntity.DESERIALIZER)
            .registerTypeAdapter(ActionEntity.DESERIALIZER)
            .registerTypeAdapter(Extra.DESERIALIZER)
            .registerTypeAdapter(ConstraintEntity.DESERIALIZER).create()
    }

    private val backupAutomatically: Flow<Boolean> = preferenceRepository
        .get(Keys.automaticBackupLocation).map { it != null }

    init {
        val doAutomaticBackup = MutableSharedFlow<AutomaticBackup>()

        coroutineScope.launch {
            doAutomaticBackup.collectLatest { backupData ->
                if (!backupAutomatically.first()) return@collectLatest

                val backupLocation = preferenceRepository.get(Keys.automaticBackupLocation).first()
                    ?: return@collectLatest

                val outputFile = fileAdapter.getFileFromUri(backupLocation)
                val result = backupAsync(
                    outputFile,
                    backupData.keyMapList,
                    backupData.fingerprintMapList,
                ).await()

                onAutomaticBackupResult.emit(result)
            }
        }

        coroutineScope.launch {
            keyMapRepository.requestBackup.collectLatest { keyMapList ->
                val backupData = AutomaticBackup(
                    keyMapList = keyMapList,
                    fingerprintMapList = fingerprintMapRepository.fingerprintMapList.firstOrNull()
                        ?.dataOrNull(),
                )

                doAutomaticBackup.emit(backupData)
            }
        }

        coroutineScope.launch {
            fingerprintMapRepository.requestBackup.collectLatest { fingerprintMaps ->
                val backupData = AutomaticBackup(
                    keyMapList = keyMapRepository.keyMapList.firstOrNull()?.dataOrNull(),
                    fingerprintMapList = fingerprintMaps,
                )

                doAutomaticBackup.emit(backupData)
            }
        }

        // automatically back up when the location changes
        preferenceRepository.get(Keys.automaticBackupLocation).drop(1).onEach {
            val keyMaps =
                keyMapRepository.keyMapList.first { it is State.Data } as State.Data

            val fingerprintMaps =
                fingerprintMapRepository.fingerprintMapList.first { it is State.Data } as State.Data

            val data = AutomaticBackup(
                keyMapList = keyMaps.data,
                fingerprintMapList = fingerprintMaps.data,
            )

            doAutomaticBackup.emit(data)
        }.launchIn(coroutineScope)
    }

    override suspend fun backupKeyMaps(uri: String, keyMapIds: List<String>): Result<String> =
        withContext(dispatchers.default()) {
            val outputFile = fileAdapter.getFileFromUri(uri)
            val allKeyMaps = keyMapRepository.keyMapList
                .first { it is State.Data } as State.Data

            val keyMapsToBackup = allKeyMaps.data.filter { keyMapIds.contains(it.uid) }

            backupAsync(outputFile, keyMapsToBackup).await().then { Success(uri) }
        }

    override suspend fun backupFingerprintMaps(uri: String): Result<String> =
        withContext(dispatchers.default()) {
            val outputFile = fileAdapter.getFileFromUri(uri)
            val fingerprintMaps =
                fingerprintMapRepository.fingerprintMapList.first { it is State.Data } as State.Data

            backupAsync(outputFile, fingerprintMaps = fingerprintMaps.data).await()
                .then { Success(uri) }
        }

    override suspend fun backupMappings(uri: String): Result<String> =
        withContext(dispatchers.default()) {
            val outputFile = fileAdapter.getFileFromUri(uri)

            val keyMaps =
                keyMapRepository.keyMapList.first { it is State.Data } as State.Data

            val fingerprintMaps =
                fingerprintMapRepository.fingerprintMapList.first { it is State.Data } as State.Data

            backupAsync(outputFile, keyMaps.data, fingerprintMaps.data).await()
                .then { Success(uri) }
        }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun restoreMappings(uri: String): Result<*> {
        return withContext(dispatchers.default()) {
            val restoreUuid = uuidGenerator.random()

            val file = fileAdapter.getFileFromUri(uri)

            val result = if (file.extension == "zip") {
                val zipDestination =
                    fileAdapter.getPrivateFile("$TEMP_RESTORE_ROOT_DIR/$restoreUuid")

                try {
                    fileAdapter.extractZipFile(file, zipDestination).then {
                        val dataJsonFile = fileAdapter.getFile(zipDestination, DATA_JSON_FILE_NAME)
                        val soundDir = fileAdapter.getFile(zipDestination, SOUNDS_DIR_NAME)

                        val inputStream = dataJsonFile.inputStream()

                        if (inputStream == null) {
                            return@withContext Error.UnknownIOError
                        }

                        val soundFiles =
                            soundDir.listFiles() ?: emptyList() // null if dir doesn't exist

                        restore(inputStream, soundFiles)
                    }
                } finally {
                    zipDestination.delete()
                }
            } else {
                val inputStream = file.inputStream()

                if (inputStream == null) {
                    return@withContext Error.UnknownIOError
                }

                restore(inputStream, emptyList())
            }

            return@withContext result
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun restore(inputStream: InputStream, soundFiles: List<IFile>): Result<*> {
        try {
            val parser = JsonParser()
            val gson = Gson()

            val rootElement = inputStream.bufferedReader().use {
                val element = parser.parse(it)

                if (element.isJsonNull) {
                    return Error.EmptyJson
                }

                element.asJsonObject
            }

            // started storing database version at db version 10
            val backupDbVersion = rootElement.get(NAME_DB_VERSION).nullInt ?: 9
            val backupAppVersion = rootElement.get(NAME_APP_VERSION).nullInt

            if (backupAppVersion != null && backupAppVersion > Constants.VERSION_CODE) {
                return Error.BackupVersionTooNew
            }

            if (backupDbVersion > AppDatabase.DATABASE_VERSION) {
                return Error.BackupVersionTooNew
            }

            val keymapListJsonArray by rootElement.byNullableArray(NAME_KEYMAP_LIST)

            val deviceInfoList by rootElement.byNullableArray("device_info")

            val migratedKeyMapList = mutableListOf<KeyMapEntity>()

            val keyMapMigrations = listOf(
                JsonMigration(9, 10) { json -> Migration9To10.migrateJson(json) },
                JsonMigration(10, 11) { json -> Migration10To11.migrateJson(json) },
                JsonMigration(11, 12) { json ->
                    Migration11To12.migrateKeyMap(json, deviceInfoList ?: JsonArray())
                },
                // do nothing because this added the log table
                JsonMigration(
                    12,
                    13,
                ) { json -> json },
            )

            keymapListJsonArray?.forEach { keyMap ->
                val migratedKeyMap = MigrationUtils.migrate(
                    keyMapMigrations,
                    inputVersion = backupDbVersion,
                    inputJson = keyMap.asJsonObject,
                    outputVersion = AppDatabase.DATABASE_VERSION,
                )
                val keyMapEntity: KeyMapEntity = gson.fromJson(migratedKeyMap)
                val keyMapWithNewId = keyMapEntity.copy(id = 0)

                migratedKeyMapList.add(keyMapWithNewId)
            }

            // Key maps with the same uid must be overwritten when restoring
            // so delete all key maps with the same uid as the ones being
            // restored
            val keyMapUids = migratedKeyMapList.map { it.uid }
            keyMapRepository.delete(*keyMapUids.toTypedArray())

            keyMapRepository.insert(*migratedKeyMapList.toTypedArray())

            val migratedFingerprintMaps = mutableListOf<FingerprintMapEntity>()

            // do nothing because this added the log table
            val newFingerprintMapMigrations = listOf(
                JsonMigration(
                    12,
                    13,
                ) { json -> json },
            )

            if (rootElement.contains(NAME_FINGERPRINT_MAP_LIST) && backupDbVersion >= 12) {
                rootElement.get(NAME_FINGERPRINT_MAP_LIST).asJsonArray.forEach { fingerprintMap ->
                    val migratedFingerprintMapJson = MigrationUtils.migrate(
                        newFingerprintMapMigrations,
                        inputVersion = backupDbVersion,
                        inputJson = fingerprintMap.asJsonObject,
                        outputVersion = AppDatabase.DATABASE_VERSION,
                    )

                    migratedFingerprintMaps.add(gson.fromJson(migratedFingerprintMapJson))
                }
            } else {
                val elementNameToGestureIdMap = mapOf(
                    "fingerprint_swipe_down" to "swipe_down",
                    "fingerprint_swipe_up" to "swipe_up",
                    "fingerprint_swipe_left" to "swipe_left",
                    "fingerprint_swipe_right" to "swipe_right",
                )

                var backupVersionTooNew = false

                elementNameToGestureIdMap.forEach { (elementName, gestureId) ->
                    val fingerprintMap by rootElement.byNullableObject(elementName)

                    fingerprintMap ?: return@forEach

                    val version by fingerprintMap!!.byInt("db_version") { 0 }
                    val isIncompatible = version > 2

                    if (isIncompatible) {
                        backupVersionTooNew = true
                    }

                    val legacyMigrations = listOf(
                        JsonMigration(0, 1) { json -> FingerprintMapMigration0To1.migrate(json) },
                        JsonMigration(1, 2) { json -> FingerprintMapMigration1To2.migrate(json) },
                        JsonMigration(2, 12) { json ->
                            Migration11To12.migrateFingerprintMap(
                                gestureId,
                                json,
                                deviceInfoList ?: JsonArray(),
                            )
                        },
                    )

                    val migratedFingerprintMapJson = MigrationUtils.migrate(
                        legacyMigrations.plus(newFingerprintMapMigrations),
                        inputVersion = version,
                        inputJson = fingerprintMap!!.asJsonObject,
                        outputVersion = AppDatabase.DATABASE_VERSION,
                    )

                    migratedFingerprintMaps.add(gson.fromJson(migratedFingerprintMapJson))
                }

                if (backupVersionTooNew) {
                    return Error.BackupVersionTooNew
                }
            }

            fingerprintMapRepository.update(*migratedFingerprintMaps.toTypedArray())

            val settingsJsonNameToPreferenceKeyMap = mapOf(
                NAME_DEFAULT_LONG_PRESS_DELAY to Keys.defaultLongPressDelay,
                NAME_DEFAULT_DOUBLE_PRESS_DELAY to Keys.defaultDoublePressDelay,
                NAME_DEFAULT_VIBRATION_DURATION to Keys.defaultVibrateDuration,
                NAME_DEFAULT_REPEAT_DELAY to Keys.defaultRepeatDelay,
                NAME_DEFAULT_REPEAT_RATE to Keys.defaultRepeatRate,
                NAME_DEFAULT_SEQUENCE_TRIGGER_TIMEOUT to Keys.defaultSequenceTriggerTimeout,
            )

            settingsJsonNameToPreferenceKeyMap.forEach { (jsonName, preferenceKey) ->
                val settingValue by rootElement.byNullableInt(jsonName)

                if (settingValue != null) {
                    preferenceRepository.set(preferenceKey, settingValue)
                }
            }

            soundFiles.forEach { file ->
                soundsManager.restoreSound(file).onFailure {
                    return it
                }
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
        output: IFile,
        keyMapList: List<KeyMapEntity>? = null,
        fingerprintMaps: List<FingerprintMapEntity>? = null,
    ): Deferred<Result<*>> = coroutineScope.async(dispatchers.io()) {
        var tempBackupDir: IFile? = null

        try {
            // delete the contents of the file
            output.clear()

            val json = gson.toJson(
                BackupModel(
                    AppDatabase.DATABASE_VERSION,
                    Constants.VERSION_CODE,
                    keyMapList,
                    fingerprintMaps,
                    defaultLongPressDelay =
                    preferenceRepository
                        .get(Keys.defaultLongPressDelay)
                        .first()
                        .takeIf { it != PreferenceDefaults.LONG_PRESS_DELAY },
                    defaultDoublePressDelay =
                    preferenceRepository
                        .get(Keys.defaultDoublePressDelay)
                        .first()
                        .takeIf { it != PreferenceDefaults.DOUBLE_PRESS_DELAY },
                    defaultRepeatDelay =
                    preferenceRepository
                        .get(Keys.defaultRepeatDelay)
                        .first()
                        .takeIf { it != PreferenceDefaults.REPEAT_DELAY },
                    defaultRepeatRate =
                    preferenceRepository
                        .get(Keys.defaultRepeatRate)
                        .first()
                        .takeIf { it != PreferenceDefaults.REPEAT_RATE },
                    defaultSequenceTriggerTimeout =
                    preferenceRepository
                        .get(Keys.defaultSequenceTriggerTimeout)
                        .first()
                        .takeIf { it != PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT },
                    defaultVibrationDuration =
                    preferenceRepository
                        .get(Keys.defaultVibrateDuration)
                        .first()
                        .takeIf { it != PreferenceDefaults.VIBRATION_DURATION },
                ),
            )

            val backupUid = uuidGenerator.random()

            tempBackupDir = fileAdapter.getPrivateFile("$TEMP_BACKUP_ROOT_DIR/$backupUid")
            tempBackupDir.createDirectory()

            val filesToBackup = mutableSetOf<IFile>()

            val dataJsonFile = fileAdapter.getFile(tempBackupDir, DATA_JSON_FILE_NAME)
            dataJsonFile.createFile()

            dataJsonFile.outputStream()?.bufferedWriter()?.use {
                it.write(json)
            }

            filesToBackup.add(dataJsonFile)

            // backup all sounds
            val soundsToBackup = soundsManager.soundFiles.value.map { it.uid }

            if (soundsToBackup.isNotEmpty()) {
                val soundsBackupDirectory = fileAdapter.getFile(tempBackupDir, SOUNDS_DIR_NAME)
                soundsBackupDirectory.createDirectory()

                soundsToBackup.forEach { soundUid ->
                    soundsManager.getSound(soundUid)
                        .then { soundFile ->
                            soundFile.copyTo(soundsBackupDirectory)
                        }.onFailure {
                            return@async it
                        }
                }

                filesToBackup.add(soundsBackupDirectory)
            }

            return@async fileAdapter.createZipFile(output, filesToBackup)
        } catch (e: Exception) {
            Timber.e(e)

            if (throwExceptions) {
                throw e
            }

            return@async Error.Exception(e)
        } finally {
            tempBackupDir?.delete()
        }
    }

    private data class AutomaticBackup(
        val keyMapList: List<KeyMapEntity>?,
        val fingerprintMapList: List<FingerprintMapEntity>?,
    )

    private data class BackupModel(
        @SerializedName(NAME_DB_VERSION)
        val dbVersion: Int,

        @SerializedName(NAME_APP_VERSION)
        val appVersion: Int,

        @SerializedName(NAME_KEYMAP_LIST)
        val keymapList: List<KeyMapEntity>? = null,

        @SerializedName(NAME_FINGERPRINT_MAP_LIST)
        val fingerprintMapList: List<FingerprintMapEntity>?,

        @SerializedName(NAME_DEFAULT_LONG_PRESS_DELAY)
        val defaultLongPressDelay: Int? = null,

        @SerializedName(NAME_DEFAULT_DOUBLE_PRESS_DELAY)
        val defaultDoublePressDelay: Int? = null,

        @SerializedName(NAME_DEFAULT_VIBRATION_DURATION)
        val defaultVibrationDuration: Int? = null,

        @SerializedName(NAME_DEFAULT_REPEAT_DELAY)
        val defaultRepeatDelay: Int? = null,

        @SerializedName(NAME_DEFAULT_REPEAT_RATE)
        val defaultRepeatRate: Int? = null,

        @SerializedName(NAME_DEFAULT_SEQUENCE_TRIGGER_TIMEOUT)
        val defaultSequenceTriggerTimeout: Int? = null,
    )
}

interface BackupManager {
    val onAutomaticBackupResult: Flow<Result<*>>

    /**
     * @return the URI to the back up
     */
    suspend fun backupKeyMaps(uri: String, keyMapIds: List<String>): Result<String>

    /**
     * @return the URI to the back up
     */
    suspend fun backupFingerprintMaps(uri: String): Result<String>

    /**
     * @return the URI to the back up
     */
    suspend fun backupMappings(uri: String): Result<String>
    suspend fun restoreMappings(uri: String): Result<*>
}
