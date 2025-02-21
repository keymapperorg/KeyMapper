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
    fun insert(vararg layout: FloatingLayoutEntity)
    fun update(vararg layout: FloatingLayoutEntity)
    suspend fun get(uid: String): FloatingLayoutEntityWithButtons?
    fun delete(vararg uid: String)
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

    override fun insert(vararg layout: FloatingLayoutEntity) {
        coroutineScope.launch(dispatchers.io()) {
            dao.insert(*layout)
        }
    }

    override fun update(vararg layout: FloatingLayoutEntity) {
        coroutineScope.launch(dispatchers.io()) {
            dao.update(*layout)
        }
    }

    override suspend fun get(uid: String): FloatingLayoutEntityWithButtons? {
        return withContext(dispatchers.io()) {
            dao.getByUidWithButtons(uid)
        }
    }

    override fun delete(vararg uid: String) {
        coroutineScope.launch(dispatchers.io()) {
            dao.deleteByUid(*uid)
        }
    }
}
