package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao
import io.github.sds100.keymapper.data.entities.FloatingButtonEntity
import io.github.sds100.keymapper.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.util.DispatcherProvider
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface FloatingButtonRepository {
    val buttonsList: Flow<State<List<FloatingButtonEntity>>>

    fun insert(vararg button: FloatingButtonEntity)
    fun update(vararg button: FloatingButtonEntity)
    suspend fun get(uid: String): FloatingButtonEntity?
    fun delete(vararg uid: String)
}

class RoomFloatingButtonRepository(
    private val dao: FloatingButtonDao,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
) : FloatingButtonRepository {
    override val buttonsList = dao.getAll()
        .map { State.Data(it) }
        .stateIn(coroutineScope, SharingStarted.Eagerly, State.Loading)

    override fun insert(vararg button: FloatingButtonEntity) {
        coroutineScope.launch(dispatchers.default()) {
            dao.insert(*button)
        }
    }

    override fun update(vararg button: FloatingButtonEntity) {
        coroutineScope.launch(dispatchers.default()) {
            dao.update(*button)
        }
    }

    override suspend fun get(uid: String): FloatingButtonEntity? = dao.getByUid(uid)

    override fun delete(vararg uid: String) {
        coroutineScope.launch(dispatchers.default()) {
            dao.deleteByUid(*uid)
        }
    }
}
