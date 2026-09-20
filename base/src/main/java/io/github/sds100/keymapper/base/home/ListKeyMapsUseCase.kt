package io.github.sds100.keymapper.base.home

import android.database.sqlite.SQLiteConstraintException
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.ActionErrorSnapshot
import io.github.sds100.keymapper.base.backup.BackupManager
import io.github.sds100.keymapper.base.backup.BackupManagerImpl
import io.github.sds100.keymapper.base.backup.BackupRestoreMappingsUseCase
import io.github.sds100.keymapper.base.backup.BackupUtils
import io.github.sds100.keymapper.base.backup.ExportedBackupLocation
import io.github.sds100.keymapper.base.constraints.Constraint
import io.github.sds100.keymapper.base.constraints.ConstraintData
import io.github.sds100.keymapper.base.constraints.ConstraintEntityMapper
import io.github.sds100.keymapper.base.constraints.ConstraintErrorSnapshot
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.constraints.ConstraintModeEntityMapper
import io.github.sds100.keymapper.base.groups.Group
import io.github.sds100.keymapper.base.groups.GroupEntityMapper
import io.github.sds100.keymapper.base.groups.GroupFamily
import io.github.sds100.keymapper.base.groups.GroupWithState
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.keymaps.KeyMapEntityMapper
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.data.entities.GroupEntity
import io.github.sds100.keymapper.data.repositories.FloatingButtonRepository
import io.github.sds100.keymapper.data.repositories.GroupRepository
import io.github.sds100.keymapper.data.repositories.KeyMapRepository
import io.github.sds100.keymapper.data.repositories.RepositoryUtils
import io.github.sds100.keymapper.system.files.FileAdapter
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class ListKeyMapsUseCaseImpl @Inject constructor(
    private val keyMapRepository: KeyMapRepository,
    private val groupRepository: GroupRepository,
    private val floatingButtonRepository: FloatingButtonRepository,
    private val fileAdapter: FileAdapter,
    private val backupManager: BackupManager,
    private val backupRestoreMappingsUseCase: BackupRestoreMappingsUseCase,
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

    override val keyMapGroup: Flow<KeyMapGroup> = keyMapListGroupUid
        .flatMapLatest(::getGroupFamily)
        .flatMapLatest { groupFamily ->
            val parentGroups = getParentsRecursively(groupFamily.group?.uid)

            getSubGroupsWithState(groupFamily.children).map { subGroups ->
                KeyMapGroup(
                    group = groupFamily.group,
                    subGroups = subGroups,
                    keyMaps = State.Loading,
                    parents = parentGroups,
                )
            }
        }
        .flatMapLatest { keyMapGroup ->
            getKeyMapsByGroup(keyMapGroup.group?.uid).map { keyMapGroup.copy(keyMaps = it) }
        }

    /**
     * Builds the enabled/error state of each subgroup from its own direct key maps and
     * constraints only, not recursively through nested subgroups.
     */
    private fun getSubGroupsWithState(groups: List<Group>): Flow<List<GroupWithState>> {
        if (groups.isEmpty()) {
            return flowOf(emptyList())
        }

        return combine(
            groups.map { group ->
                combine(
                    getKeyMapsByGroup(group.uid),
                    actionErrorSnapshot,
                    constraintErrorSnapshot,
                ) { keyMapsState, actionSnapshot, constraintSnapshot ->
                    buildGroupWithState(
                        group,
                        keyMapsState.dataOrNull() ?: emptyList(),
                        actionSnapshot,
                        constraintSnapshot,
                    )
                }
            },
        ) { it.toList() }
    }

    private fun buildGroupWithState(
        group: Group,
        keyMaps: List<KeyMap>,
        actionErrorSnapshot: ActionErrorSnapshot,
        constraintErrorSnapshot: ConstraintErrorSnapshot,
    ): GroupWithState {
        val isEnabled = keyMaps.isEmpty() || keyMaps.any { it.isEnabled }

        val hasOwnConstraintError = group.constraintState.constraints.any {
            constraintErrorSnapshot.getError(it) != null
        }

        val hasEnabledKeyMapError = keyMaps.any { keyMap ->
            keyMap.isEnabled &&
                (
                    actionErrorSnapshot.getErrors(
                        keyMap.actionList.filter { it.isEnabled }.map { it.data },
                    ).values.any { it != null } ||
                        keyMap.constraintState.constraints.any {
                            constraintErrorSnapshot.getError(it) != null
                        }
                    )
        }

        return GroupWithState(
            group = group,
            isEnabled = isEnabled,
            isError = hasOwnConstraintError || hasEnabledKeyMapError,
        )
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
                if (siblings.any { sibling ->
                        sibling.uid != group.uid &&
                            sibling.name == renamedGroup.name
                    }
                ) {
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
        val parentUid = currentGroup?.parentUid

        // If stuck in a non existent group, or the parent is null then pop to the root.
        if (parentUid == null) {
            setCurrentGroup(null)
        } else {
            // Check if the group exists.
            val group = groupRepository.getGroup(parentUid) ?: return
            setCurrentGroup(group.uid)
        }
    }

    override suspend fun addGroupConstraint(constraintData: ConstraintData) {
        keyMapListGroupUid.value?.also { groupUid ->
            val constraint = Constraint(data = constraintData)
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

            val constraintGroups = group.constraintState.groups
                .map { constraintGroup ->
                    constraintGroup.copy(
                        constraints = constraintGroup.constraints.filterNot {
                            it.uid == constraintUid
                        },
                    )
                }
                .filter { it.constraints.isNotEmpty() }

            group =
                group.copy(constraintState = group.constraintState.copy(groups = constraintGroups))

            try {
                groupRepository.update(GroupEntityMapper.toEntity(group))
            } catch (_: SQLiteConstraintException) {
                return
            }
        }
    }

    override suspend fun toggleGroupConstraintNot(constraintUid: String) {
        keyMapListGroupUid.value?.also { groupUid ->
            val groupEntity = groupRepository.getGroup(groupUid) ?: return
            var group = GroupEntityMapper.fromEntity(groupEntity)

            val constraintGroups = group.constraintState.groups.map { constraintGroup ->
                constraintGroup.copy(
                    constraints = constraintGroup.constraints.map { constraint ->
                        if (constraint.uid == constraintUid) {
                            constraint.copy(isNot = !constraint.isNot)
                        } else {
                            constraint
                        }
                    },
                )
            }

            group =
                group.copy(constraintState = group.constraintState.copy(groups = constraintGroups))

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

    override fun enableGroupKeyMaps() {
        keyMapListGroupUid.value?.also { groupUid ->
            keyMapRepository.enableByGroup(groupUid)
        }
    }

    override fun disableGroupKeyMaps() {
        keyMapListGroupUid.value?.also { groupUid ->
            keyMapRepository.disableByGroup(groupUid)
        }
    }

    private fun getKeyMapsByGroup(groupUid: String?): Flow<State<List<KeyMap>>> {
        return combine(
            keyMapRepository.getByGroup(groupUid),
            floatingButtonRepository.buttonsList,
        ) { keyMapList, buttonListState ->
            val buttonList = when (buttonListState) {
                is State.Loading -> {
                    return@combine State.Loading
                }

                is State.Data -> buttonListState.data
            }

            val keyMaps = withContext(Dispatchers.Default) {
                keyMapList.map { keyMap ->
                    KeyMapEntityMapper.fromEntity(keyMap, buttonList)
                }
            }

            State.Data(keyMaps)
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

    override suspend fun backupKeyMaps(vararg uid: String): KMResult<ExportedBackupLocation> {
        val fileName = BackupUtils.createBackupFileName()

        // Share in private files so the share sheet can show the file name. This is some quirk
        // of the storage access framework https://issuetracker.google.com/issues/268079113.
        // Saving it directly to Downloads with the MediaStore returns a content URI
        // that only contains a numerical ID, not the file name.
        val file = fileAdapter.getPrivateFile("${BackupManagerImpl.BACKUP_DIR}/$fileName")
        file.createFile()
        backupManager.backupKeyMaps(file, uid.asList())

        return backupRestoreMappingsUseCase.exportToUserAccessibleLocation(file, fileName)
    }
}

interface ListKeyMapsUseCase : DisplayKeyMapUseCase {
    val keyMapGroup: Flow<KeyMapGroup>

    suspend fun newGroup()
    suspend fun openGroup(uid: String?)
    suspend fun popGroup()
    suspend fun deleteGroup()
    suspend fun renameGroup(name: String): Boolean
    suspend fun addGroupConstraint(constraintData: ConstraintData)
    suspend fun removeGroupConstraint(constraintUid: String)
    suspend fun toggleGroupConstraintNot(constraintUid: String)
    suspend fun setGroupConstraintMode(mode: ConstraintMode)
    fun getGroups(parentUid: String?): Flow<List<Group>>
    fun enableGroupKeyMaps()
    fun disableGroupKeyMaps()

    val selectionGroupFamily: Flow<GroupFamily>
    suspend fun openSelectionGroup(uid: String?)
    fun moveKeyMapsToGroup(groupUid: String?, vararg keyMapUids: String)
    fun moveKeyMapsToSelectedGroup(vararg keyMapUids: String)
    suspend fun moveKeyMapsToNewGroup(vararg keyMapUids: String)

    fun deleteKeyMap(vararg uid: String)
    fun enableKeyMap(vararg uid: String)
    fun disableKeyMap(vararg uid: String)
    fun duplicateKeyMap(vararg uid: String)
    suspend fun backupKeyMaps(vararg uid: String): KMResult<ExportedBackupLocation>
}
