package io.github.sds100.keymapper.backup

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
import com.google.gson.stream.MalformedJsonException
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.actions.sound.SoundsManager
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.EntityExtra
import io.github.sds100.keymapper.data.entities.FingerprintMapEntity
import io.github.sds100.keymapper.data.entities.FloatingButtonEntity
import io.github.sds100.keymapper.data.entities.FloatingButtonKeyEntity
import io.github.sds100.keymapper.data.entities.FloatingLayoutEntity
import io.github.sds100.keymapper.data.entities.GroupEntity
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.data.entities.TriggerEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity
import io.github.sds100.keymapper.data.migration.JsonMigration
import io.github.sds100.keymapper.data.migration.Migration10To11
import io.github.sds100.keymapper.data.migration.Migration11To12
import io.github.sds100.keymapper.data.migration.Migration9To10
import io.github.sds100.keymapper.data.migration.MigrationUtils
import io.github.sds100.keymapper.data.migration.fingerprintmaps.FingerprintMapMigration0To1
import io.github.sds100.keymapper.data.migration.fingerprintmaps.FingerprintMapMigration1To2
import io.github.sds100.keymapper.data.migration.fingerprintmaps.FingerprintToKeyMapMigration
import io.github.sds100.keymapper.data.repositories.FloatingButtonRepository
import io.github.sds100.keymapper.data.repositories.FloatingLayoutRepository
import io.github.sds100.keymapper.data.repositories.GroupRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.data.repositories.RepositoryUtils
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
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.then
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/**
 * Created by sds100 on 16/03/2021.
 */

class BackupManagerImpl(
    private val coroutineScope: CoroutineScope,
    private val fileAdapter: FileAdapter,
    private val keyMapRepository: KeyMapRepository,
    private val preferenceRepository: PreferenceRepository,
    private val floatingLayoutRepository: FloatingLayoutRepository,
    private val floatingButtonRepository: FloatingButtonRepository,
    private val groupRepository: GroupRepository,
    private val soundsManager: SoundsManager,
    private val throwExceptions: Boolean = false,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    private val uuidGenerator: UuidGenerator = DefaultUuidGenerator(),
) : BackupManager {

    companion object {
        // DON'T CHANGE THIS.
        private const val DATA_JSON_FILE_NAME = "data.json"
        private const val SOUNDS_DIR_NAME = "sounds"

        // This is where completed back ups are stored in private app data.
        // IMPORTANT! This must be the same as the path in res/xml/provider_paths.xml
        // so when sharing back up files other apps can read them.
        const val BACKUP_DIR = "backups"

        // This is where the temp files for creating a back up are stored.
        private const val TEMP_BACKUP_ROOT_DIR = "backup_temp"
        private const val TEMP_RESTORE_ROOT_DIR = "restore_temp"
    }

    override val onAutomaticBackupResult = MutableSharedFlow<Result<*>>()

    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(FingerprintMapEntity.DESERIALIZER)
            .registerTypeAdapter(KeyMapEntity.DESERIALIZER)
            .registerTypeAdapter(TriggerEntity.DESERIALIZER)
            .registerTypeAdapter(TriggerKeyEntity.SERIALIZER)
            .registerTypeAdapter(TriggerKeyEntity.DESERIALIZER)
            .registerTypeAdapter(ActionEntity.DESERIALIZER)
            .registerTypeAdapter(EntityExtra.DESERIALIZER)
            .registerTypeAdapter(ConstraintEntity.DESERIALIZER)
            .registerTypeAdapter(FloatingLayoutEntity.DESERIALIZER)
            .registerTypeAdapter(FloatingButtonEntity.DESERIALIZER)
            .registerTypeAdapter(GroupEntity.DESERIALIZER)
            .create()
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
                val result = backupAsync(outputFile, backupData.keyMapList)

                onAutomaticBackupResult.emit(result)
            }
        }

        coroutineScope.launch {
            keyMapRepository.requestBackup.collectLatest { keyMapList ->
                val backupData = AutomaticBackup(keyMapList = keyMapList)

                doAutomaticBackup.emit(backupData)
            }
        }

        // automatically back up when the location changes
        preferenceRepository.get(Keys.automaticBackupLocation).drop(1).onEach {
            val keyMaps =
                keyMapRepository.keyMapList.first { it is State.Data } as State.Data

            val data = AutomaticBackup(keyMapList = keyMaps.data)

            doAutomaticBackup.emit(data)
        }.launchIn(coroutineScope)
    }

    override suspend fun backupKeyMaps(output: IFile, keyMapIds: List<String>): Result<Unit> {
        return withContext(dispatchers.default()) {
            val allKeyMaps = keyMapRepository.keyMapList
                .filterIsInstance<State.Data<List<KeyMapEntity>>>()
                .first()

            val keyMapsToBackup = allKeyMaps.data.filter { keyMapIds.contains(it.uid) }

            backupAsync(output, keyMapsToBackup)

            Success(Unit)
        }
    }

    override suspend fun backupEverything(output: IFile): Result<Unit> {
        return withContext(dispatchers.io()) {
            val keyMaps =
                keyMapRepository.keyMapList
                    .filterIsInstance<State.Data<List<KeyMapEntity>>>()
                    .first()

            val groups = groupRepository.getAllGroups().first()

            backupAsync(output, keyMaps.data, groups)

            Success(Unit)
        }
    }

    override suspend fun getBackupContent(file: IFile): Result<BackupContent> {
        return extractFile(file).then { extractedDir ->

            val dataJsonFile = fileAdapter.getFile(extractedDir, DATA_JSON_FILE_NAME)

            val inputStream = dataJsonFile.inputStream()

            if (inputStream == null) {
                return Error.UnknownIOError
            }

            return parseBackupContent(inputStream)
        }
    }

    private suspend fun parseBackupContent(jsonFile: InputStream): Result<BackupContent> = withContext(dispatchers.io()) {
        try {
            val rootElement = jsonFile.bufferedReader().use {
                val element = JsonParser().parse(it)

                if (element.isJsonNull) {
                    return@withContext Error.EmptyJson
                }

                element.asJsonObject
            }

            val backupDbVersion = rootElement.get(BackupContent.NAME_DB_VERSION).nullInt ?: 9
            val backupAppVersion = rootElement.get(BackupContent.NAME_APP_VERSION).nullInt

            if (backupAppVersion != null && backupAppVersion > Constants.VERSION_CODE) {
                return@withContext Error.BackupVersionTooNew
            }

            if (backupDbVersion > AppDatabase.DATABASE_VERSION) {
                return@withContext Error.BackupVersionTooNew
            }

            val keyMapListJsonArray by rootElement.byNullableArray(BackupContent.NAME_KEYMAP_LIST)

            val deviceInfoList by rootElement.byNullableArray(BackupContent.NAME_DEVICE_INFO)

            val migratedKeyMapList = mutableListOf<KeyMapEntity>()

            val keyMapMigrations = listOf(
                JsonMigration(9, 10) { json -> Migration9To10.migrateJson(json) },
                JsonMigration(10, 11) { json -> Migration10To11.migrateJson(json) },
                JsonMigration(11, 12) { json ->
                    Migration11To12.migrateKeyMap(json, deviceInfoList ?: JsonArray())
                },
                // do nothing because this added the log table
                JsonMigration(12, 13) { json -> json },

                // Do nothing because this just add the floating layouts table and indexes.
                JsonMigration(13, 14) { json -> json },

                // Do nothing just added floating button entity columns
                JsonMigration(14, 15) { json -> json },

                // Do nothing just added nullable group uid column
                JsonMigration(15, 16) { json -> json },
            )

            if (keyMapListJsonArray != null) {
                for (keyMap in keyMapListJsonArray!!) {
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
            }

            val migratedFingerprintMaps = mutableListOf<FingerprintMapEntity>()

            // do nothing because this added the log table
            val newFingerprintMapMigrations = listOf(
                JsonMigration(12, 13) { json -> json },
            )

            if (rootElement.contains(BackupContent.NAME_FINGERPRINT_MAP_LIST) && backupDbVersion >= 12) {
                rootElement.get(BackupContent.NAME_FINGERPRINT_MAP_LIST).asJsonArray.forEach { fingerprintMap ->
                    val migratedFingerprintMapJson = MigrationUtils.migrate(
                        newFingerprintMapMigrations,
                        inputVersion = backupDbVersion,
                        inputJson = fingerprintMap.asJsonObject,
                        outputVersion = 13,
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
                        JsonMigration(
                            0,
                            1,
                        ) { json -> FingerprintMapMigration0To1.migrate(json) },
                        JsonMigration(
                            1,
                            2,
                        ) { json -> FingerprintMapMigration1To2.migrate(json) },
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
                        outputVersion = 13,
                    )

                    migratedFingerprintMaps.add(gson.fromJson(migratedFingerprintMapJson))
                }

                if (backupVersionTooNew) {
                    return@withContext Error.BackupVersionTooNew
                }
            }

            for (entity in migratedFingerprintMaps) {
                FingerprintToKeyMapMigration.migrate(entity)?.let { migratedKeyMapList.add(it) }
            }

            val defaultLongPressDelay by rootElement.byNullableInt(BackupContent.NAME_DEFAULT_LONG_PRESS_DELAY)
            val defaultDoublePressDelay by rootElement.byNullableInt(BackupContent.NAME_DEFAULT_DOUBLE_PRESS_DELAY)
            val defaultVibrationDuration by rootElement.byNullableInt(BackupContent.NAME_DEFAULT_VIBRATION_DURATION)
            val defaultRepeatDelay by rootElement.byNullableInt(BackupContent.NAME_DEFAULT_REPEAT_DELAY)
            val defaultRepeatRate by rootElement.byNullableInt(BackupContent.NAME_DEFAULT_REPEAT_RATE)
            val defaultSequenceTriggerTimeout by rootElement.byNullableInt(BackupContent.NAME_DEFAULT_SEQUENCE_TRIGGER_TIMEOUT)

            val floatingLayoutsJson by rootElement.byNullableArray(BackupContent.NAME_FLOATING_LAYOUTS)
            val floatingLayouts: List<FloatingLayoutEntity>? =
                floatingLayoutsJson?.map { json -> gson.fromJson(json) }

            val floatingButtonsJson by rootElement.byNullableArray(BackupContent.NAME_FLOATING_BUTTONS)
            val floatingButtons: List<FloatingButtonEntity>? =
                floatingButtonsJson?.map { json -> gson.fromJson(json) }

            val groupsJson by rootElement.byNullableArray(BackupContent.NAME_GROUPS)
            val groups: List<GroupEntity> =
                groupsJson?.map { json -> gson.fromJson(json) } ?: emptyList()

            val content = BackupContent(
                dbVersion = backupDbVersion,
                appVersion = backupAppVersion,
                keyMapList = migratedKeyMapList,
                defaultLongPressDelay = defaultLongPressDelay,
                defaultDoublePressDelay = defaultDoublePressDelay,
                defaultVibrationDuration = defaultVibrationDuration,
                defaultRepeatDelay = defaultRepeatDelay,
                defaultRepeatRate = defaultRepeatRate,
                defaultSequenceTriggerTimeout = defaultSequenceTriggerTimeout,
                floatingLayouts = floatingLayouts,
                floatingButtons = floatingButtons,
                groups = groups,
            )

            return@withContext Success(content)
        } catch (e: MalformedJsonException) {
            return@withContext Error.CorruptJsonFile(e.message ?: "")
        } catch (e: JsonSyntaxException) {
            return@withContext Error.CorruptJsonFile(e.message ?: "")
        } catch (e: NoSuchElementException) {
            return@withContext Error.CorruptJsonFile(e.message ?: "")
        } catch (e: Exception) {
            Timber.e(e)

            if (throwExceptions) {
                throw e
            }

            return@withContext Error.Exception(e)
        }
    }

    override suspend fun restore(file: IFile, restoreType: RestoreType): Result<*> {
        return extractFile(file).then { extractedDir ->

            val dataJsonFile = fileAdapter.getFile(extractedDir, DATA_JSON_FILE_NAME)

            val inputStream = dataJsonFile.inputStream()

            if (inputStream == null) {
                return@then Error.UnknownIOError
            }

            val soundDir = fileAdapter.getFile(extractedDir, SOUNDS_DIR_NAME)
            val soundFiles = soundDir.listFiles() ?: emptyList() // null if dir doesn't exist

            return@then parseBackupContent(inputStream).then { backupContent ->
                restore(restoreType, backupContent, soundFiles)
            }
        }
    }

    /**
     * @return the directory with the extracted contents.
     */
    private suspend fun extractFile(file: IFile): Result<IFile> {
        val restoreUuid = uuidGenerator.random()

        val zipDestination =
            fileAdapter.getPrivateFile("$TEMP_RESTORE_ROOT_DIR/$restoreUuid")

        try {
            if (file.extension == "zip") {
                withContext(dispatchers.io()) {
                    fileAdapter.extractZipFile(file, zipDestination)
                }
            } else {
                withContext(dispatchers.io()) {
                    file.copyTo(zipDestination, DATA_JSON_FILE_NAME)
                }
            }

            return Success(zipDestination)
        } catch (e: IOException) {
            return Error.UnknownIOError
        }
    }

    private suspend fun restore(
        restoreType: RestoreType,
        backupContent: BackupContent,
        soundFiles: List<IFile>,
    ): Result<*> {
        try {
            // MUST come before restoring key maps so it is possible to
            // validate that each key map's group exists in the repository.
            if (backupContent.groups != null) {
                val groupUids = backupContent.groups.map { it.uid }.toMutableSet()

                groupRepository.getAllGroups().first()
                    .map { it.uid }
                    .toSet()
                    .also { groupUids.addAll(it) }

                val currentTime = System.currentTimeMillis()

                for (group in backupContent.groups) {
                    // Set the last opened date to now so that the imported group
                    // shows as the most recent.
                    var modifiedGroup = group.copy(lastOpenedDate = currentTime)

                    // If the group's parent wasn't backed up or doesn't exist
                    // then set it the parent to the root group
                    if (!groupUids.contains(group.parentUid)) {
                        modifiedGroup = modifiedGroup.copy(parentUid = null)
                    }

                    RepositoryUtils.saveUniqueName(
                        modifiedGroup,
                        saveBlock = { groupRepository.insert(it) },
                        renameBlock = { entity, suffix ->
                            entity.copy(name = "${entity.name} $suffix")
                        },
                    )
                }
            }

            if (backupContent.keyMapList != null) {
                val groups = groupRepository.getAllGroups().first()
                val keyMapList = validateKeyMapGroups(backupContent.keyMapList, groups)

                when (restoreType) {
                    RestoreType.APPEND ->
                        appendKeyMapsInRepository(keyMapList)

                    RestoreType.REPLACE ->
                        replaceKeyMapsInRepository(keyMapList)
                }
            }

            if (backupContent.defaultLongPressDelay != null) {
                preferenceRepository.set(
                    Keys.defaultLongPressDelay,
                    backupContent.defaultLongPressDelay,
                )
            }

            if (backupContent.defaultDoublePressDelay != null) {
                preferenceRepository.set(
                    Keys.defaultDoublePressDelay,
                    backupContent.defaultDoublePressDelay,
                )
            }

            if (backupContent.defaultVibrationDuration != null) {
                preferenceRepository.set(
                    Keys.defaultVibrateDuration,
                    backupContent.defaultVibrationDuration,
                )
            }

            if (backupContent.defaultRepeatDelay != null) {
                preferenceRepository.set(Keys.defaultRepeatDelay, backupContent.defaultRepeatDelay)
            }

            if (backupContent.defaultRepeatRate != null) {
                preferenceRepository.set(Keys.defaultRepeatRate, backupContent.defaultRepeatRate)
            }

            if (backupContent.defaultSequenceTriggerTimeout != null) {
                preferenceRepository.set(
                    Keys.defaultSequenceTriggerTimeout,
                    backupContent.defaultSequenceTriggerTimeout,
                )
            }

            soundFiles.forEach { file ->
                soundsManager.restoreSound(file).onFailure {
                    return it
                }
            }

            if (backupContent.floatingLayouts != null) {
                for (layout in backupContent.floatingLayouts) {
                    RepositoryUtils.saveUniqueName(
                        layout,
                        saveBlock = { floatingLayoutRepository.insert(it) },
                        renameBlock = { entity, suffix ->
                            entity.copy(name = "${entity.name} $suffix")
                        },
                    )
                }
            }

            if (backupContent.floatingButtons != null) {
                floatingButtonRepository.insert(*backupContent.floatingButtons.toTypedArray())
            }

            return Success(Unit)
        } catch (e: MalformedJsonException) {
            return Error.CorruptJsonFile(e.message ?: "")
        } catch (e: JsonSyntaxException) {
            return Error.CorruptJsonFile(e.message ?: "")
        } catch (e: NoSuchElementException) {
            return Error.CorruptJsonFile(e.message ?: "")
        } catch (e: Exception) {
            Timber.e(e)

            if (throwExceptions) {
                throw e
            }

            return Error.Exception(e)
        }
    }

    private suspend fun appendKeyMapsInRepository(keyMaps: List<KeyMapEntity>) = withContext(dispatchers.default()) {
        val randomUids = keyMaps.map { it.copy(uid = UUID.randomUUID().toString()) }
        keyMapRepository.insert(*randomUids.toTypedArray())
    }

    private suspend fun replaceKeyMapsInRepository(keyMaps: List<KeyMapEntity>) = withContext(dispatchers.default()) {
        keyMapRepository.deleteAll()
        keyMapRepository.insert(*keyMaps.toTypedArray())
    }

    /**
     * Check whether the group each key map is assigned to actually exists. If it does not
     * then move it to the root group by setting the group uid to null.
     */
    private fun validateKeyMapGroups(
        keyMaps: List<KeyMapEntity>,
        groups: List<GroupEntity>,
    ): List<KeyMapEntity> {
        val groupMap = groups.associateBy { it.uid }

        return keyMaps.map { keyMap ->
            if (keyMap.groupUid == null || groupMap.containsKey(keyMap.groupUid)) {
                keyMap
            } else {
                keyMap.copy(groupUid = null)
            }
        }
    }

    private suspend fun backupAsync(
        output: IFile,
        keyMapList: List<KeyMapEntity>? = null,
        extraGroups: List<GroupEntity> = emptyList(),
    ): Result<IFile> {
        return withContext(dispatchers.io()) {
            val backupUid = uuidGenerator.random()

            val tempBackupDir = fileAdapter.getPrivateFile("$TEMP_BACKUP_ROOT_DIR/$backupUid")
            tempBackupDir.createDirectory()

            try {
                // delete the contents of the file
                output.clear()

                val floatingLayouts: MutableList<FloatingLayoutEntity> = mutableListOf()
                val floatingButtons: MutableList<FloatingButtonEntity> = mutableListOf()
                val groupMap: MutableMap<String, GroupEntity> = mutableMapOf()

                if (keyMapList != null) {
                    val floatingButtonTriggerKeys = keyMapList
                        .flatMap { it.trigger.keys }
                        .filterIsInstance<FloatingButtonKeyEntity>()
                        .map { it.buttonUid }
                        .distinct()

                    for (buttonUid in floatingButtonTriggerKeys) {
                        val buttonWithLayout = floatingButtonRepository.get(buttonUid) ?: continue

                        if (floatingLayouts.none { it.uid == buttonWithLayout.layout.uid }) {
                            floatingLayouts.add(buttonWithLayout.layout)
                        }

                        floatingButtons.add(buttonWithLayout.button)
                    }

                    for (keyMap in keyMapList) {
                        val groupUid = keyMap.groupUid ?: continue
                        if (!groupMap.containsKey(groupUid)) {
                            val groupEntity = groupRepository.getGroup(groupUid) ?: continue
                            groupMap[groupUid] = groupEntity
                        }
                    }

                    for (group in extraGroups) {
                        if (!groupMap.containsKey(group.uid)) {
                            groupMap[group.uid] = group
                        }
                    }
                }

                val backupContent = BackupContent(
                    AppDatabase.DATABASE_VERSION,
                    Constants.VERSION_CODE,
                    keyMapList,
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
                    floatingLayouts = floatingLayouts.takeIf { it.isNotEmpty() },
                    floatingButtons = floatingButtons.takeIf { it.isNotEmpty() },
                    groups = groupMap.values.toList(),
                )

                val json = gson.toJson(backupContent)

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
                                return@withContext it
                            }
                    }

                    filesToBackup.add(soundsBackupDirectory)
                }

                return@withContext fileAdapter.createZipFile(output, filesToBackup)
                    .then { Success(output) }
            } catch (e: Exception) {
                Timber.e(e)

                if (throwExceptions) {
                    throw e
                }

                return@withContext Error.Exception(e)
            }
        }
    }

    private data class AutomaticBackup(
        val keyMapList: List<KeyMapEntity>?,
    )
}

interface BackupManager {
    val onAutomaticBackupResult: Flow<Result<*>>

    suspend fun getBackupContent(file: IFile): Result<BackupContent>

    /**
     * @return the URI to the back up
     */
    suspend fun backupKeyMaps(output: IFile, keyMapIds: List<String>): Result<Unit>

    /**
     * @return the URI to the back up file which can then be used for sharing.
     */
    suspend fun backupEverything(output: IFile): Result<Unit>
    suspend fun restore(file: IFile, restoreType: RestoreType): Result<*>
}
