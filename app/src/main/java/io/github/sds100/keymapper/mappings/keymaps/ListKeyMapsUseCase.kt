package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.backup.BackupManager
import io.github.sds100.keymapper.backup.BackupManagerImpl
import io.github.sds100.keymapper.backup.BackupUtils
import io.github.sds100.keymapper.data.repositories.FloatingButtonRepository
import io.github.sds100.keymapper.data.repositories.GroupRepository
import io.github.sds100.keymapper.groups.GroupEntityMapper
import io.github.sds100.keymapper.groups.GroupWithSubGroups
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.dataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Created by sds100 on 16/04/2021.
 */
class ListKeyMapsUseCaseImpl(
    private val keyMapRepository: KeyMapRepository,
    private val groupRepository: GroupRepository,
    private val floatingButtonRepository: FloatingButtonRepository,
    private val fileAdapter: FileAdapter,
    private val backupManager: BackupManager,
    displayKeyMapUseCase: DisplayKeyMapUseCase,
) : ListKeyMapsUseCase,
    DisplayKeyMapUseCase by displayKeyMapUseCase {

    private val groupUid = MutableStateFlow<String?>(null)

    override suspend fun openGroup(uid: String) {
        // Check if the group exists.
        val group = groupRepository.getGroup(uid) ?: return
        groupUid.update { group.uid }
    }

    override suspend fun popGroup() {
        val currentGroupUid = groupUid.value ?: return
        val currentGroup = groupRepository.getGroup(currentGroupUid)

        // If stuck in a non existent group, or the parent is null then pop to the root.
        if (currentGroup?.parentUid == null) {
            groupUid.value = null
        } else {
            // Check if the group exists.
            val group = groupRepository.getGroup(currentGroup.parentUid) ?: return
            groupUid.update { group.uid }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val group: Flow<GroupWithSubGroups> = groupUid.flatMapLatest { groupUid ->
        if (groupUid == null) {
            groupRepository.getGroupsByParent(null).map { subGroupEntities ->
                val subGroups = subGroupEntities.map(GroupEntityMapper::fromEntity)
                GroupWithSubGroups(group = null, subGroups = subGroups)
            }
        } else {
            groupRepository.getGroupWithSubGroups(groupUid).map { groupWithSubGroups ->
                val group = GroupEntityMapper.fromEntity(groupWithSubGroups.group)
                val subGroups =
                    groupWithSubGroups.subGroups.map(GroupEntityMapper::fromEntity)

                GroupWithSubGroups(group, subGroups)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val keyMapGroup: Flow<KeyMapGroup> = channelFlow {
        group
            .map { group ->
                KeyMapGroup(
                    group = group.group,
                    subGroups = group.subGroups,
                    keyMaps = State.Loading,
                )
            }
            .onEach { send(it) }
            .flatMapLatest { keyMapGroup ->
                getKeyMapsByGroup(keyMapGroup.group?.uid).map { keyMapGroup.copy(keyMaps = it) }
            }.collect {
                send(it)
            }
    }

    private fun getKeyMapsByGroup(groupUid: String?): Flow<State<List<KeyMap>>> = channelFlow {
        send(State.Loading)

        combine(
            keyMapRepository.getByGroup(groupUid),
            floatingButtonRepository.buttonsList,
        ) { keyMapList, buttonListState ->
            Pair(keyMapList, buttonListState)
        }.collectLatest { (keyMapList, buttonListState) ->
            if (buttonListState is State.Loading) {
                send(State.Loading)
            }

            val buttonList = buttonListState.dataOrNull() ?: return@collectLatest

            val keyMaps = withContext(Dispatchers.Default) {
                keyMapList.map { keyMap ->
                    KeyMapEntityMapper.fromEntity(keyMap, buttonList)
                }
            }

            send(State.Data(keyMaps))
        }
    }

    override fun deleteKeyMap(vararg uid: String) {
        keyMapRepository.delete(*uid)
    }

    override fun enableKeyMap(vararg uid: String) {
        keyMapRepository.enableById(*uid)
    }

    override fun disableKeyMap(vararg uid: String) {
        keyMapRepository.disableById(*uid)
    }

    override fun duplicateKeyMap(vararg uid: String) {
        keyMapRepository.duplicate(*uid)
    }

    override suspend fun backupKeyMaps(vararg uid: String): Result<String> {
        val fileName = BackupUtils.createBackupFileName()

        // Share in private files so the share sheet can show the file name. This is some quirk
        // of the storage access framework https://issuetracker.google.com/issues/268079113.
        // Saving it directly to Downloads with the MediaStore returns a content URI
        // that only contains a numerical ID, not the file name.
        return fileAdapter.getPrivateFile("${BackupManagerImpl.BACKUP_DIR}/$fileName").let { file ->
            file.createFile()
            backupManager.backupKeyMaps(file, uid.asList())
            Success(fileAdapter.getPublicUriForPrivateFile(file))
        }
    }
}

interface ListKeyMapsUseCase : DisplayKeyMapUseCase {
    val keyMapGroup: Flow<KeyMapGroup>

    suspend fun openGroup(uid: String)
    suspend fun popGroup()

    fun deleteKeyMap(vararg uid: String)
    fun enableKeyMap(vararg uid: String)
    fun disableKeyMap(vararg uid: String)
    fun duplicateKeyMap(vararg uid: String)
    suspend fun backupKeyMaps(vararg uid: String): Result<String>
}
