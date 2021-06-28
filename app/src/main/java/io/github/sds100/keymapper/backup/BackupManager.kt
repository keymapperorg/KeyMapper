package io.github.sds100.keymapper.backup

import androidx.annotation.VisibleForTesting
import com.github.salomonbrys.kotson.*
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.MalformedJsonException
import io.github.sds100.keymapper.actions.SoundAction
import io.github.sds100.keymapper.actions.sound.SoundsManager
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.migration.*
import io.github.sds100.keymapper.data.migration.fingerprintmaps.FingerprintMapMigration_0_1
import io.github.sds100.keymapper.data.migration.fingerprintmaps.FingerprintMapMigration_1_2
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.Mapping
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapEntity
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapEntityMapper
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapRepository
import io.github.sds100.keymapper.mappings.keymaps.KeyMapEntity
import io.github.sds100.keymapper.mappings.keymaps.KeyMapEntityMapper
import io.github.sds100.keymapper.mappings.keymaps.KeyMapRepository
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*


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
) : BackupManager {

    companion object {
        //DON'T CHANGE THESE. Used for serialization and parsing.
        @VisibleForTesting
        private const val NAME_DB_VERSION = "keymap_db_version"
        private const val NAME_KEYMAP_LIST = "keymap_list"
        private const val NAME_FINGERPRINT_MAP_LIST = "fingerprint_map_list"
        private const val NAME_DEFAULT_LONG_PRESS_DELAY = "default_long_press_delay"
        private const val NAME_DEFAULT_DOUBLE_PRESS_DELAY = "default_double_press_delay"
        private const val NAME_DEFAULT_VIBRATION_DURATION = "default_vibration_duration"
        private const val NAME_DEFAULT_REPEAT_DELAY = "default_repeat_delay"
        private const val NAME_DEFAULT_REPEAT_RATE = "default_repeat_rate"
        private const val NAME_DEFAULT_SEQUENCE_TRIGGER_TIMEOUT = "default_sequence_trigger_timeout"

        //DON'T CHANGE THIS.
        private const val DATA_JSON_FILE_NAME = "data.json"
        private const val SOUNDS_DIR_NAME = "sounds"

        private const val TEMP_BACKUP_ROOT_DIR = "backup_temp"
    }

    override val onBackupResult = MutableSharedFlow<Result<*>>()
    override val onRestoreResult = MutableSharedFlow<Result<*>>()
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

                val result = fileAdapter
                    .openOutputStream(backupLocation)
                    .suspendThen { outputStream ->
                        backupAsync(
                            outputStream,
                            backupData.keyMapList,
                            backupData.fingerprintMapList
                        ).await()
                    }

                onAutomaticBackupResult.emit(result)
            }
        }

        coroutineScope.launch {
            keyMapRepository.requestBackup.collectLatest { keyMapList ->
                val backupData = AutomaticBackup(
                    keyMapList = keyMapList,
                    fingerprintMapList = fingerprintMapRepository.fingerprintMapList.firstOrNull()
                        ?.dataOrNull()
                )

                doAutomaticBackup.emit(backupData)
            }
        }

        coroutineScope.launch {
            fingerprintMapRepository.requestBackup.collectLatest { fingerprintMaps ->
                val backupData = AutomaticBackup(
                    keyMapList = keyMapRepository.keyMapList.firstOrNull()?.dataOrNull(),
                    fingerprintMapList = fingerprintMaps
                )

                doAutomaticBackup.emit(backupData)
            }
        }

        //automatically back up when the location changes
        preferenceRepository.get(Keys.automaticBackupLocation).drop(1).onEach {
            val keyMaps =
                keyMapRepository.keyMapList.first { it is State.Data } as State.Data

            val fingerprintMaps =
                fingerprintMapRepository.fingerprintMapList.first { it is State.Data } as State.Data

            val data = AutomaticBackup(
                keyMapList = keyMaps.data,
                fingerprintMapList = fingerprintMaps.data
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

                    backupAsync(outputStream, keyMapsToBackup).await()
                }

            onBackupResult.emit(result)
        }
    }

    override fun backupFingerprintMaps(uri: String) {
        coroutineScope.launch(dispatchers.default()) {
            val result = fileAdapter
                .openOutputStream(uri)
                .suspendThen { outputStream ->
                    val fingerprintMaps =
                        fingerprintMapRepository.fingerprintMapList.first { it is State.Data } as State.Data

                    backupAsync(outputStream, fingerprintMaps = fingerprintMaps.data).await()
                }

            onBackupResult.emit(result)
        }
    }

    override fun backupMappings(uri: String) {
        coroutineScope.launch(dispatchers.default()) {
            val result = fileAdapter
                .openOutputStream(uri)
                .suspendThen { outputStream ->
                    val keyMaps =
                        keyMapRepository.keyMapList.first { it is State.Data } as State.Data

                    val fingerprintMaps =
                        fingerprintMapRepository.fingerprintMapList.first { it is State.Data } as State.Data

                    backupAsync(
                        outputStream,
                        keyMaps.data,
                        fingerprintMaps.data
                    ).await()
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

    @Suppress("DEPRECATION")
    private fun restore(backupJson: String): Result<*> {
        try {
            val parser = JsonParser()
            val gson = Gson()

            if (backupJson.isBlank()) return Error.EmptyJson

            val rootElement = parser.parse(backupJson).asJsonObject

            //started storing database version at db version 10
            val backupDbVersion = rootElement.get(NAME_DB_VERSION).nullInt ?: 9

            val keymapListJsonArray by rootElement.byNullableArray(NAME_KEYMAP_LIST)

            if (backupDbVersion > AppDatabase.DATABASE_VERSION) {
                return Error.BackupVersionTooNew
            }

            val deviceInfoList by rootElement.byNullableArray("device_info")

            val migratedKeyMapList = mutableListOf<KeyMapEntity>()

            val keyMapMigrations = listOf(
                JsonMigration(9, 10) { json -> Migration_9_10.migrateJson(json) },
                JsonMigration(10, 11) { json -> Migration_10_11.migrateJson(json) },
                JsonMigration(11, 12) { json ->
                    Migration_11_12.migrateKeyMap(json, deviceInfoList ?: JsonArray())
                },
                JsonMigration(12, 13) { json -> json } //do nothing because this added the log table
            )

            keymapListJsonArray?.forEach { keyMap ->
                val migratedKeyMap = MigrationUtils.migrate(
                    keyMapMigrations,
                    inputVersion = backupDbVersion,
                    inputJson = keyMap.asJsonObject,
                    outputVersion = AppDatabase.DATABASE_VERSION
                )
                val keyMapEntity: KeyMapEntity = gson.fromJson(migratedKeyMap)
                val keyMapWithNewId = keyMapEntity.copy(id = 0, uid = UUID.randomUUID().toString())

                migratedKeyMapList.add(keyMapWithNewId)
            }

            keyMapRepository.insert(*migratedKeyMapList.toTypedArray())

            val migratedFingerprintMaps = mutableListOf<FingerprintMapEntity>()

            val newFingerprintMapMigrations = listOf(
                JsonMigration(12, 13) { json -> json } //do nothing because this added the log table
            )

            if (rootElement.contains(NAME_FINGERPRINT_MAP_LIST) && backupDbVersion >= 12) {

                rootElement.get(NAME_FINGERPRINT_MAP_LIST).asJsonArray.forEach { fingerprintMap ->
                    val migratedFingerprintMapJson = MigrationUtils.migrate(
                        newFingerprintMapMigrations,
                        inputVersion = backupDbVersion,
                        inputJson = fingerprintMap.asJsonObject,
                        outputVersion = AppDatabase.DATABASE_VERSION
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
                        JsonMigration(0, 1) { json -> FingerprintMapMigration_0_1.migrate(json) },
                        JsonMigration(1, 2) { json -> FingerprintMapMigration_1_2.migrate(json) },
                        JsonMigration(2, 12) { json ->
                            Migration_11_12.migrateFingerprintMap(
                                gestureId,
                                json,
                                deviceInfoList ?: JsonArray()
                            )
                        }
                    )

                    val migratedFingerprintMapJson = MigrationUtils.migrate(
                        legacyMigrations.plus(newFingerprintMapMigrations),
                        inputVersion = version,
                        inputJson = fingerprintMap!!.asJsonObject,
                        outputVersion = AppDatabase.DATABASE_VERSION
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
        backupOutputStream: OutputStream,
        keyMapList: List<KeyMapEntity>? = null,
        fingerprintMaps: List<FingerprintMapEntity>? = null
    ) = coroutineScope.async(dispatchers.io()) {
        var tempBackupDirectory: String? = null

        try {
            //delete the contents of the file
            if (backupOutputStream is FileOutputStream) {
                backupOutputStream.channel.truncate(0)
            }

            val json = gson.toJson(
                BackupModel(
                    AppDatabase.DATABASE_VERSION,
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
                )
            )

            val backupUid = UUID.randomUUID().toString()

            tempBackupDirectory = "$TEMP_BACKUP_ROOT_DIR/$backupUid"
            fileAdapter.createPrivateDirectory(tempBackupDirectory).onFailure {
                return@async it
            }

            val filesToBackup = mutableSetOf<String>()

            val dataJsonFile = "$tempBackupDirectory/$DATA_JSON_FILE_NAME"

            fileAdapter.createPrivateFile(dataJsonFile)
                .then { outputStream ->
                    outputStream.bufferedWriter().use {
                        it.write(json)
                    }
                    Success(Unit)
                }
                .onFailure {
                    return@async it
                }

            filesToBackup.add(dataJsonFile)

            //backup all sounds
            val soundsToBackup = soundsManager.soundFiles.value.map { it.uid }

            if (soundsToBackup.isNotEmpty()) {
                val soundsBackupDirectory = "$tempBackupDirectory/$SOUNDS_DIR_NAME"
                fileAdapter.createPrivateDirectory(soundsBackupDirectory).onFailure {
                    return@async it
                }

                soundsToBackup.forEach { soundUid ->
                    soundsManager.getSound(soundUid).then { sound ->

                        fileAdapter.createPrivateFile("$soundsBackupDirectory/$soundUid")
                            .onSuccess { soundBackup ->
                                sound.copyTo(soundBackup)
                                sound.close()
                                soundBackup.close()
                            }
                    }.onFailure {
                        return@async it
                    }
                }

                filesToBackup.add(soundsBackupDirectory)
            }

            fileAdapter.createZipFile(backupOutputStream, filesToBackup)

            return@async Success(Unit)
        } catch (e: Exception) {
            Timber.e(e)

            if (throwExceptions) {
                throw e
            }

            return@async Error.Exception(e)
        } finally {
            if (tempBackupDirectory != null) {
                fileAdapter.deletePrivateDirectory(tempBackupDirectory)
            }
        }
    }

    private data class AutomaticBackup(
        val keyMapList: List<KeyMapEntity>?,
        val fingerprintMapList: List<FingerprintMapEntity>?
    )

    private data class BackupModel(
        @SerializedName(NAME_DB_VERSION)
        val dbVersion: Int,

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
        val defaultSequenceTriggerTimeout: Int? = null
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