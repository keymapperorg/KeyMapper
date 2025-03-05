package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.db.dao.FloatingLayoutDao
import io.github.sds100.keymapper.data.entities.FloatingLayoutEntity
import io.github.sds100.keymapper.data.entities.FloatingLayoutEntityWithButtons
import io.github.sds100.keymapper.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.util.DispatcherProvider
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface FloatingLayoutRepository {
    val layouts: Flow<State<List<FloatingLayoutEntityWithButtons>>>
    suspend fun insert(vararg layout: FloatingLayoutEntity)
    fun update(vararg layout: FloatingLayoutEntity)
    fun get(uid: String): Flow<FloatingLayoutEntityWithButtons?>
    fun delete(vararg uid: String)
    suspend fun count(): Int
}

class RoomFloatingLayoutRepository(
    private val dao: FloatingLayoutDao,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
) : FloatingLayoutRepository {
    override val layouts = dao.getAllWithButtons()
        .map { State.Data(it) }
        .flowOn(dispatchers.io())
        .stateIn(coroutineScope, SharingStarted.Eagerly, State.Loading)

    override suspend fun insert(vararg layout: FloatingLayoutEntity) {
        withContext(dispatchers.io()) {
            dao.insert(*layout)
        }
    }

    override fun update(vararg layout: FloatingLayoutEntity) {
        coroutineScope.launch(dispatchers.io()) {
            dao.update(*layout)
        }
    }

    override fun get(uid: String): Flow<FloatingLayoutEntityWithButtons?> {
        return dao.getByUidWithButtonsFlow(uid)
    }

    override fun delete(vararg uid: String) {
        coroutineScope.launch(dispatchers.io()) {
            dao.deleteByUid(*uid)
        }
    }

    override suspend fun count(): Int {
        return dao.count()
    }
}
