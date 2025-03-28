package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.db.dao.GroupDao
import io.github.sds100.keymapper.data.entities.GroupEntity
import io.github.sds100.keymapper.data.entities.GroupEntityWithSubGroups
import io.github.sds100.keymapper.data.entities.KeyMapEntitiesWithGroup
import io.github.sds100.keymapper.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.util.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun getKeyMapsByGroup(groupUid: String): Flow<KeyMapEntitiesWithGroup>
    suspend fun getGroup(uid: String): GroupEntity?
    fun getGroupsByParent(uid: String?): Flow<List<GroupEntity>>
    fun getGroupWithSubGroups(uid: String): Flow<GroupEntityWithSubGroups>
}

class RoomGroupRepository(
    private val dao: GroupDao,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
) : GroupRepository {
    override fun getKeyMapsByGroup(groupUid: String): Flow<KeyMapEntitiesWithGroup> {
        return dao.getKeyMapsByGroup(groupUid)
    }

    override suspend fun getGroup(uid: String): GroupEntity? {
        return dao.getById(uid)
    }

    override fun getGroupsByParent(uid: String?): Flow<List<GroupEntity>> {
        return dao.getGroupsByParent(uid)
    }

    override fun getGroupWithSubGroups(uid: String): Flow<GroupEntityWithSubGroups> {
        return dao.getGroupWithSubGroups(uid)
    }
}
