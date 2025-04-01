package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.db.dao.GroupDao
import io.github.sds100.keymapper.data.entities.GroupEntity
import io.github.sds100.keymapper.data.entities.GroupEntityWithChildren
import io.github.sds100.keymapper.data.entities.KeyMapEntitiesWithGroup
import io.github.sds100.keymapper.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.util.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface GroupRepository {
    val groups: Flow<List<GroupEntity>>

    fun getKeyMapsByGroup(groupUid: String): Flow<KeyMapEntitiesWithGroup>
    suspend fun getGroup(uid: String): GroupEntity?
    fun getAllGroups(): Flow<List<GroupEntity>>
    fun getGroups(vararg uid: String): Flow<List<GroupEntity>>
    fun getGroupsByParent(uid: String?): Flow<List<GroupEntity>>
    fun getGroupWithChildren(uid: String): Flow<GroupEntityWithChildren>
    suspend fun insert(groupEntity: GroupEntity)
    suspend fun update(groupEntity: GroupEntity)
    fun delete(uid: String)
    suspend fun setLastOpenedDate(groupUid: String, timestamp: Long)
}

class RoomGroupRepository(
    private val dao: GroupDao,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
) : GroupRepository {

    override val groups: StateFlow<List<GroupEntity>> =
        dao.getAll().stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override fun getKeyMapsByGroup(groupUid: String): Flow<KeyMapEntitiesWithGroup> {
        return dao.getKeyMapsByGroup(groupUid).flowOn(dispatchers.io())
    }

    override suspend fun getGroup(uid: String): GroupEntity? {
        return withContext(dispatchers.io()) { dao.getById(uid) }
    }

    override fun getAllGroups(): Flow<List<GroupEntity>> {
        return dao.getAll().flowOn(dispatchers.io())
    }

    override fun getGroups(vararg uid: String): Flow<List<GroupEntity>> {
        return dao.getManyByIdFlow(*uid).flowOn(dispatchers.io())
    }

    override fun getGroupsByParent(uid: String?): Flow<List<GroupEntity>> {
        return dao.getGroupsByParent(uid).flowOn(dispatchers.io())
    }

    override fun getGroupWithChildren(uid: String): Flow<GroupEntityWithChildren> {
        return dao.getGroupWithSubGroups(uid).flowOn(dispatchers.io())
    }

    override suspend fun insert(groupEntity: GroupEntity) {
        withContext(dispatchers.io()) {
            dao.insert(groupEntity)
        }
    }

    override suspend fun update(groupEntity: GroupEntity) {
        withContext(dispatchers.io()) {
            dao.update(groupEntity)
        }
    }

    override fun delete(uid: String) {
        coroutineScope.launch {
            withContext(dispatchers.io()) {
                dao.deleteByUid(uid)
            }
        }
    }

    override suspend fun setLastOpenedDate(groupUid: String, timestamp: Long) {
        withContext(dispatchers.io()) {
            dao.setLastOpenedDate(groupUid, timestamp)
        }
    }
}
