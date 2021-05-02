package io.github.sds100.keymapper.backup

import androidx.annotation.VisibleForTesting
import com.github.salomonbrys.kotson.*
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.MalformedJsonException
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.entities.TriggerEntity
import io.github.sds100.keymapper.data.migration.*
import io.github.sds100.keymapper.data.migration.fingerprintmaps.FingerprintMapMigration_0_1
import io.github.sds100.keymapper.data.migration.fingerprintmaps.FingerprintMapMigration_1_2
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapEntity
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapRepository
import io.github.sds100.keymapper.mappings.keymaps.KeyMapEntity
import io.github.sds100.keymapper.mappings.keymaps.KeyMapRepository
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val throwExceptions: Boolean = false,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BackupManager {

    companion object {
        //DON'T CHANGE THESE. Used for serialization and parsing.
        @VisibleForTesting
        private const val NAME_DB_VERSION = "keymap_db_version"
        private const val NAME_KEYMAP_LIST = "keymap_list"
        private const val NAME_FINGERPRINT_MAP_LIST = "fingerprint_map_list"
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
                val backupData = BackupData(
                    keyMapList = keyMapList,
                    fingerprintMaps = fingerprintMapRepository.fingerprintMapList.firstOrNull()
                        ?.dataOrNull()
                )

                doAutomaticBackup.emit(backupData)
            }
        }

        coroutineScope.launch {
            fingerprintMapRepository.requestBackup.collectLatest { fingerprintMaps ->
                val backupData = BackupData(
                    keyMapList = keyMapRepository.keyMapList.firstOrNull()?.dataOrNull(),
                    fingerprintMaps = fingerprintMaps
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

            val data = BackupData(
                keyMapList = keyMaps.data,
                fingerprintMaps = fingerprintMaps.data
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
                    val fingerprintMaps =
                        fingerprintMapRepository.fingerprintMapList.first { it is State.Data } as State.Data

                    val data = BackupData(fingerprintMaps = fingerprintMaps.data)

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
                    val keyMaps =
                        keyMapRepository.keyMapList.first { it is State.Data } as State.Data

                    val fingerprintMaps =
                        fingerprintMapRepository.fingerprintMapList.first { it is State.Data } as State.Data

                    val data = BackupData(
                        keyMapList = keyMaps.data,
                        fingerprintMaps = fingerprintMaps.data
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

            val newFingerprintMapMigrations = listOf<JsonMigration>(
                //no migrations yet
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

            outputStream.bufferedWriter().use { writer ->

                val json = gson.toJson(
                    BackupModel(
                        AppDatabase.DATABASE_VERSION,
                        data.keyMapList,
                        data.fingerprintMaps,
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
        val fingerprintMaps: List<FingerprintMapEntity>? = null
    )

    private data class BackupModel(
        @SerializedName(NAME_DB_VERSION)
        val dbVersion: Int,

        @SerializedName(NAME_KEYMAP_LIST)
        val keymapList: List<KeyMapEntity>? = null,

        @SerializedName(NAME_FINGERPRINT_MAP_LIST)
        val fingerprintMapList: List<FingerprintMapEntity>?,
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