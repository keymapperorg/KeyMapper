package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.db.dao.ViewIdDao
import io.github.sds100.keymapper.data.entities.ViewIdEntity
import io.github.sds100.keymapper.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.util.DispatcherProvider
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomViewIdRepository(
    private val dao: ViewIdDao,
    private val coroutineScope: CoroutineScope,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider()
): ViewIdRepository  {

    override val viewIdList = dao.getAll()
        .map { State.Data(it) }
        .flowOn(dispatchers.default())
        .stateIn(coroutineScope, SharingStarted.Eagerly, State.Loading)

    init {
        // clean up the DB on every app start
        /*coroutineScope.launch(Dispatchers.Default) {
            dao.deleteAll()
        }*/
    }

    override fun insert(entry: ViewIdEntity) {
        coroutineScope.launch(Dispatchers.Default) {
            if (!dao.viewIdExists(entry.fullName)) {
                dao.insert(entry)
            }
        }
    }

    override fun deleteAll() {
        coroutineScope.launch(Dispatchers.Default) {
            dao.deleteAll()
        }
    }

    override suspend fun getAll() {
        coroutineScope.launch(Dispatchers.Default) {
            dao.getAll()
        }
    }
}

interface ViewIdRepository {
    val viewIdList: Flow<State<List<ViewIdEntity>>>
    fun insert(entry: ViewIdEntity)
    fun deleteAll()
    suspend fun getAll()
}