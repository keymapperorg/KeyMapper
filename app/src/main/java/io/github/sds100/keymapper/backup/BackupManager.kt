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
import io.github.sds100.keymapper.common.result.Error
import io.github.sds100.keymapper.common.result.Result
import io.github.sds100.keymapper.common.result.Success
import io.github.sds100.keymapper.common.result.onFailure
import io.github.sds100.keymapper.common.result.then
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
import io.github.sds100.keymapper.data.entities.FloatingLayoutEntityWithButtons
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
import io.github.sds100.keymapper.keymaps.KeyMapRepository
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.IFile
import io.github.sds100.keymapper.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.util.DefaultUuidGenerator
import io.github.sds100.keymapper.util.DispatcherProvider
import io.github.sds100.keymapper.common.state.State
import io.github.sds100.keymapper.util.TreeNode
import io.github.sds100.keymapper.util.UuidGenerator
import io.github.sds100.keymapper.util.breadFirstTraversal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.util.LinkedList
import java.util.UUID

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
        coroutineScope.launch {
            combine(
                backupAutomatically,
                preferenceRepository.get(Keys.automaticBackupLocation),
                keyMapRepository.keyMapList.filterIsInstance<State.Data<List<KeyMapEntity>>>(),
                groupRepository.getAllGroups(),
                floatingLayoutRepository.layouts.filterIsInstance<State.Data<List<FloatingLayoutEntityWithButtons>>>(),
            ) { backupAutomatically, location, keyMaps, groups, floatingLayouts ->
                if (!backupAutomatically) {
                    return@combine
                }

                location ?: return@combine

                val outputFile = fileAdapter.getFileFromUri(location)
                val result = backupAsync(outputFile, keyMaps.data, groups, floatingLayouts.data)
                onAutomaticBackupResult.emit(result)
            }.collect()
        }
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

            val layouts = floatingLayoutRepository.layouts
                .filterIsInstance<State.Data<List<FloatingLayoutEntityWithButtons>>>()
                .first()

            backupAsync(output, keyMaps.data, groups, layouts.data)

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

                // Do nothing just added nullable column for when a group was last opened
                JsonMigration(16, 17) { json -> json },

                // Do nothing. It just removed the group name index.
                JsonMigration(17, 18) { json -> json },

                // Do nothing. Just added the accessibility node table.
                JsonMigration(18, 19) { json -> json },

                // Do nothing. Just added columns to the accessibility node table.
                JsonMigration(19, 20) { json -> json },
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
                restore(
                    restoreType,
                    backupContent,
                    soundFiles,
                    currentTime = System.currentTimeMillis(),
                )
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

    suspend fun restore(
        restoreType: RestoreType,
        backupContent: BackupContent,
        soundFiles: List<IFile>,
        currentTime: Long,
    ): Result<*> {
        try {
            // MUST come before restoring key maps so it is possible to
            // validate that each key map's group exists in the repository.
            if (backupContent.groups != null) {
                val existingGroupUids = groupRepository.getAllGroups().first()
                    .map { it.uid }
                    .toSet()

                val groupUids = backupContent.groups.map { it.uid }.toMutableSet()

                groupUids.addAll(existingGroupUids)

                // Group parents must be restored first so an SqliteConstraintException
                // is not thrown when restoring a child group.
                val groupRestoreTrees = buildGroupTrees(backupContent.groups)

                for (tree in groupRestoreTrees) {
                    tree.breadFirstTraversal { group ->
                        restoreGroup(group, currentTime, groupUids, existingGroupUids)
                    }
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

    private suspend fun restoreGroup(
        group: GroupEntity,
        currentTime: Long,
        groupUids: Set<String>,
        existingGroupUids: Set<String>,
    ) {
        // Set the last opened date to now so that the imported group
        // shows as the most recent.
        var modifiedGroup = group.copy(lastOpenedDate = currentTime)

        // If the group's parent wasn't backed up or doesn't exist
        // then set it the parent to the root group
        if (!groupUids.contains(group.parentUid)) {
            modifiedGroup = modifiedGroup.copy(parentUid = null)
        }

        val siblings =
            groupRepository.getGroupsByParent(modifiedGroup.parentUid).first()

        modifiedGroup = RepositoryUtils.saveUniqueName(
            modifiedGroup,
            saveBlock = { renamedGroup ->
                // Do not rename the group with a (1) if it is the same UID. Just overwrite the name.
                if (siblings.any { sibling -> sibling.uid != renamedGroup.uid && sibling.name == renamedGroup.name }) {
                    throw IllegalStateException("Non unique group name")
                }
            },
            renameBlock = { entity, suffix ->
                entity.copy(name = "${entity.name} $suffix")
            },
        )

        if (existingGroupUids.contains(modifiedGroup.uid)) {
            groupRepository.update(modifiedGroup)
        } else {
            groupRepository.insert(modifiedGroup)
        }
    }

    /**
     * Converts the group relationships into trees. This first finds all the root groups which
     * have no parent. Then it loops over all the other groups indefinitely until they have been
     * added to their parent. If the parent does not exist while looping then it is skipped and
     * processed in the next iteration.
     *
     * @return A list of the root nodes for all the group trees.
     */
    private fun buildGroupTrees(groups: List<GroupEntity>): List<TreeNode<GroupEntity>> {
        if (groups.isEmpty()) {
            return emptyList()
        }

        val nodeMap = mutableMapOf<String, TreeNode<GroupEntity>>()
        val rootNodes = mutableListOf<TreeNode<GroupEntity>>()

        val groupQueue = LinkedList<GroupEntity>()

        for (group in groups) {
            if (group.parentUid == null) {
                val node = TreeNode(group)
                nodeMap[group.uid] = node
                rootNodes.add(node)
            } else {
                groupQueue.add(group)
            }
        }

        while (groupQueue.isNotEmpty()) {
            val groupsToRemove = mutableListOf<GroupEntity>()

            for (group in groupQueue) {
                if (nodeMap.containsKey(group.parentUid)) {
                    val node = TreeNode(group)
                    nodeMap[group.uid] = node
                    nodeMap[group.parentUid]!!.children.add(node)
                    groupsToRemove.add(group)
                }
            }

            groupQueue.removeAll(groupsToRemove.toSet())
        }

        return rootNodes
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
        keyMapList: List<KeyMapEntity> = emptyList(),
        extraGroups: List<GroupEntity> = emptyList(),
        extraLayouts: List<FloatingLayoutEntityWithButtons> = emptyList(),
    ): Result<IFile> {
        return withContext(dispatchers.io()) {
            val backupUid = uuidGenerator.random()

            val tempBackupDir = fileAdapter.getPrivateFile("$TEMP_BACKUP_ROOT_DIR/$backupUid")
            tempBackupDir.createDirectory()

            try {
                // delete the contents of the file
                output.clear()

                val backupContent = createBackupContent(keyMapList, extraGroups, extraLayouts)

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

    suspend fun createBackupContent(
        keyMapList: List<KeyMapEntity>,
        extraGroups: List<GroupEntity>,
        extraLayouts: List<FloatingLayoutEntityWithButtons>,
    ): BackupContent {
        val floatingLayoutsMap: MutableMap<String, FloatingLayoutEntity> = mutableMapOf()
        val floatingButtonsMap: MutableMap<String, FloatingButtonEntity> = mutableMapOf()
        val groupMap: MutableMap<String, GroupEntity> = mutableMapOf()

        val floatingButtonTriggerKeys = keyMapList
            .flatMap { it.trigger.keys }
            .filterIsInstance<FloatingButtonKeyEntity>()
            .map { it.buttonUid }
            .distinct()

        for (buttonUid in floatingButtonTriggerKeys) {
            val buttonWithLayout = floatingButtonRepository.get(buttonUid) ?: continue
            val layoutUid = buttonWithLayout.layout.uid

            if (!floatingLayoutsMap.containsKey(layoutUid)) {
                floatingLayoutsMap[layoutUid] = buttonWithLayout.layout
            }

            if (!floatingButtonsMap.containsKey(buttonUid)) {
                floatingButtonsMap[buttonUid] = buttonWithLayout.button
            }
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

        for (layoutWithButtons in extraLayouts) {
            if (!floatingLayoutsMap.containsKey(layoutWithButtons.layout.uid)) {
                floatingLayoutsMap[layoutWithButtons.layout.uid] = layoutWithButtons.layout
            }

            for (button in layoutWithButtons.buttons) {
                if (!floatingButtonsMap.containsKey(button.uid)) {
                    floatingButtonsMap[button.uid] = button
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
            floatingLayouts = floatingLayoutsMap.values.toList().takeIf { it.isNotEmpty() },
            floatingButtons = floatingButtonsMap.values.toList().takeIf { it.isNotEmpty() },
            groups = groupMap.values.toList().takeIf { it.isNotEmpty() },
        )
        return backupContent
    }
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
