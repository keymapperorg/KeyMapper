package io.github.sds100.keymapper.base.groups

import io.github.sds100.keymapper.data.repositories.GroupRepository
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class SelectGroupUseCaseImpl @Inject constructor(private val groupRepository: GroupRepository) :
    SelectGroupUseCase {

    private val _selectionGroupUid = MutableStateFlow<String?>(null)
    override val selectionGroupUid: StateFlow<String?> = _selectionGroupUid.asStateFlow()

    override val selectionGroupFamily: Flow<GroupFamily> =
        selectionGroupUid.flatMapLatest(::getGroupFamily)

    override suspend fun openSelectionGroup(uid: String?) {
        if (uid == null) {
            // If null then open the root group.
            _selectionGroupUid.update { null }
        } else {
            // Check if the group exists.
            val group = groupRepository.getGroup(uid) ?: return
            _selectionGroupUid.update { group.uid }
        }
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
}
