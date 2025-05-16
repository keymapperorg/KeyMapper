package io.github.sds100.keymapper.keymaps

import android.database.sqlite.SQLiteConstraintException
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.backup.BackupManager
import io.github.sds100.keymapper.backup.BackupManagerImpl
import io.github.sds100.keymapper.backup.BackupUtils
import io.github.sds100.keymapper.common.result.Result
import io.github.sds100.keymapper.common.result.Success
import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.constraints.ConstraintEntityMapper
import io.github.sds100.keymapper.constraints.ConstraintMode
import io.github.sds100.keymapper.constraints.ConstraintModeEntityMapper
import io.github.sds100.keymapper.data.entities.GroupEntity
import io.github.sds100.keymapper.data.repositories.FloatingButtonRepository
import io.github.sds100.keymapper.data.repositories.GroupRepository
import io.github.sds100.keymapper.data.repositories.RepositoryUtils
import io.github.sds100.keymapper.groups.Group
import io.github.sds100.keymapper.groups.GroupEntityMapper
import io.github.sds100.keymapper.groups.GroupFamily
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.common.state.State
import io.github.sds100.keymapper.common.state.dataOrNull
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.LinkedList

@OptIn(ExperimentalCoroutinesApi::class)
class ListKeyMapsUseCaseImpl(
    private val keyMapRepository: KeyMapRepository,
    private val groupRepository: GroupRepository,
    private val floatingButtonRepository: FloatingButtonRepository,
    private val fileAdapter: FileAdapter,
    private val backupManager: BackupManager,
    private val resourceProvider: ResourceProvider,
    displayKeyMapUseCase: DisplayKeyMapUseCase,
) : ListKeyMapsUseCase,
    DisplayKeyMapUseCase by displayKeyMapUseCase {
    private val keyMapListGroupUid = MutableStateFlow<String?>(null)
    private val selectionGroupUid = MutableStateFlow<String?>(null)

    private fun setCurrentGroup(groupUid: String?) {
        keyMapListGroupUid.update { groupUid }
        selectionGroupUid.update { groupUid }
    }

    private suspend fun getGroupFamily(groupUid: String?): Flow<GroupFamily> {
        // If the current group is the root then just get the subgroups.
        if (groupUid == null) {
            return groupRepository.getGroupsByParent(null).map { childrenEntities ->
                val children = childrenEntities
                    .map(GroupEntityMapper::fromEntity)
                    .sortedByDescending { it.lastOpenedDate }
                GroupFamily(group = null, children = children, parents = emptyList())
            }
        } else {
            val parents = getParentsRecursively(groupUid)

            return groupRepository.getGroupWithChildren(groupUid).map { groupWithChildren ->
                val group = GroupEntityMapper.fromEntity(groupWithChildren.group)
                val children = groupWithChildren.children.map(GroupEntityMapper::fromEntity)

                GroupFamily(group, children = children, parents = parents)
            }
        }
    }

    override val keyMapGroup: Flow<KeyMapGroup> = channelFlow {
        keyMapListGroupUid
            .flatMapLatest(::getGroupFamily)
            .map { groupFamily ->
                val parentGroups = getParentsRecursively(groupFamily.group?.uid)

                KeyMapGroup(
                    group = groupFamily.group,
                    subGroups = groupFamily.children,
                    keyMaps = State.Loading,
                    parents = parentGroups,
                )
            }.onEach { send(it) }
            .flatMapLatest { keyMapGroup ->
                getKeyMapsByGroup(keyMapGroup.group?.uid).map { keyMapGroup.copy(keyMaps = it) }
            }.collect {
                send(it)
            }
    }

    override val selectionGroupFamily: Flow<GroupFamily> =
        selectionGroupUid.flatMapLatest(::getGroupFamily)

    override suspend fun openSelectionGroup(uid: String?) {
        if (uid == null) {
            // If null then open the root group.
            selectionGroupUid.update { null }
        } else {
            // Check if the group exists.
            val group = groupRepository.getGroup(uid) ?: return
            selectionGroupUid.update { group.uid }
        }
    }

    private suspend fun getParentsRecursively(groupUid: String?): List<Group> {
        val list = LinkedList<Group>()
        var count = 0

        if (groupUid == null) {
            return emptyList()
        }

        var currentGroup: String? = groupRepository.getGroup(groupUid)?.parentUid

        while (count < 1000) {
            if (currentGroup == null) {
                break
            }

            val group = groupRepository.getGroup(currentGroup) ?: break
            list.addFirst(GroupEntityMapper.fromEntity(group))
            currentGroup = group.parentUid

            count++
        }

        return list
    }

    override fun getGroups(parentUid: String?): Flow<List<Group>> {
        return groupRepository.getGroupsByParent(parentUid)
            .map { list -> list.map(GroupEntityMapper::fromEntity) }
    }

    override suspend fun newGroup() {
        val newGroup = createNewGroup()
        setCurrentGroup(newGroup.uid)
    }

    override suspend fun moveKeyMapsToNewGroup(vararg keyMapUids: String) {
        val newGroup = createNewGroup()
        moveKeyMapsToGroup(newGroup.uid, *keyMapUids)
        setCurrentGroup(newGroup.uid)
    }

    private suspend fun createNewGroup(): GroupEntity {
        val defaultName = resourceProvider.getString(R.string.default_group_name)
        var group = GroupEntity(
            parentUid = keyMapListGroupUid.value,
            name = defaultName,
            lastOpenedDate = System.currentTimeMillis(),
        )

        group = ensureUniqueName(group)
        groupRepository.insert(group)
        return group
    }

    override suspend fun deleteGroup() {
        keyMapListGroupUid.value?.also { groupUid ->
            val group = groupRepository.getGroup(groupUid) ?: return

            setCurrentGroup(group.parentUid)

            groupRepository.delete(groupUid)
        }
    }

    override suspend fun renameGroup(name: String): Boolean {
        if (name.isBlank()) {
            return true
        }

        keyMapListGroupUid.value?.also { groupUid ->
            var entity = groupRepository.getGroup(groupUid) ?: return true

            entity = entity.copy(name = name.trim())

            val siblings = groupRepository.getGroupsByParent(entity.parentUid).first()

            if (siblings.any { it.uid != groupUid && it.name == entity.name }) {
                return false
            }

            groupRepository.update(entity)
        }

        return true
    }

    private suspend fun ensureUniqueName(group: GroupEntity): GroupEntity {
        val siblings = groupRepository.getGroupsByParent(group.parentUid).first()

        return RepositoryUtils.saveUniqueName(
            entity = group,
            saveBlock = { renamedGroup ->
                if (siblings.any { sibling -> sibling.uid != group.uid && sibling.name == renamedGroup.name }) {
                    throw IllegalStateException("Non unique group name")
                }
            },
            renameBlock = { entity, suffix ->
                entity.copy(name = "${entity.name} $suffix")
            },
        )
    }

    override suspend fun openGroup(uid: String?) {
        if (uid == null) {
            // If null then open the root group.
            setCurrentGroup(null)
        } else {
            // Check if the group exists.
            val group = groupRepository.getGroup(uid) ?: return
            setCurrentGroup(group.uid)
            groupRepository.setLastOpenedDate(group.uid, System.currentTimeMillis())
        }
    }

    override suspend fun popGroup() {
        val currentGroupUid = keyMapListGroupUid.value ?: return
        val currentGroup = groupRepository.getGroup(currentGroupUid)

        // If stuck in a non existent group, or the parent is null then pop to the root.
        if (currentGroup?.parentUid == null) {
            setCurrentGroup(null)
        } else {
            // Check if the group exists.
            val group = groupRepository.getGroup(currentGroup.parentUid) ?: return
            setCurrentGroup(group.uid)
        }
    }

    override suspend fun addGroupConstraint(constraint: Constraint) {
        keyMapListGroupUid.value?.also { groupUid ->
            val constraintEntity = ConstraintEntityMapper.toEntity(constraint)
            var groupEntity = groupRepository.getGroup(groupUid) ?: return

            groupEntity = groupEntity.copy(
                constraintList = groupEntity.constraintList.plus(constraintEntity),
            )

            try {
                groupRepository.update(groupEntity)
            } catch (_: SQLiteConstraintException) {
                return
            }
        }
    }

    override suspend fun setGroupConstraintMode(mode: ConstraintMode) {
        keyMapListGroupUid.value?.also { groupUid ->
            val group = groupRepository.getGroup(groupUid) ?: return

            val groupEntity = group.copy(constraintMode = ConstraintModeEntityMapper.toEntity(mode))

            try {
                groupRepository.update(groupEntity)
            } catch (_: SQLiteConstraintException) {
                return
            }
        }
    }

    override suspend fun removeGroupConstraint(constraintUid: String) {
        keyMapListGroupUid.value?.also { groupUid ->
            val groupEntity = groupRepository.getGroup(groupUid) ?: return
            var group = GroupEntityMapper.fromEntity(groupEntity)

            val constraints = group.constraintState.constraints
                .filterNot { it.uid == constraintUid }
                .toSet()

            group =
                group.copy(constraintState = group.constraintState.copy(constraints = constraints))

            try {
                groupRepository.update(GroupEntityMapper.toEntity(group))
            } catch (_: SQLiteConstraintException) {
                return
            }
        }
    }

    override fun moveKeyMapsToGroup(groupUid: String?, vararg keyMapUids: String) {
        keyMapRepository.moveToGroup(groupUid, *keyMapUids)
    }

    override fun moveKeyMapsToSelectedGroup(vararg keyMapUids: String) {
        keyMapRepository.moveToGroup(selectionGroupUid.value, *keyMapUids)
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

    suspend fun newGroup()
    suspend fun openGroup(uid: String?)
    suspend fun popGroup()
    suspend fun deleteGroup()
    suspend fun renameGroup(name: String): Boolean
    suspend fun addGroupConstraint(constraint: Constraint)
    suspend fun removeGroupConstraint(constraintUid: String)
    suspend fun setGroupConstraintMode(mode: ConstraintMode)
    fun getGroups(parentUid: String?): Flow<List<Group>>

    val selectionGroupFamily: Flow<GroupFamily>
    suspend fun openSelectionGroup(uid: String?)
    fun moveKeyMapsToGroup(groupUid: String?, vararg keyMapUids: String)
    fun moveKeyMapsToSelectedGroup(vararg keyMapUids: String)
    suspend fun moveKeyMapsToNewGroup(vararg keyMapUids: String)

    fun deleteKeyMap(vararg uid: String)
    fun enableKeyMap(vararg uid: String)
    fun disableKeyMap(vararg uid: String)
    fun duplicateKeyMap(vararg uid: String)
    suspend fun backupKeyMaps(vararg uid: String): Result<String>
}
