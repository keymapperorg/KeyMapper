package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.db.dao.LogEntryDao
import io.github.sds100.keymapper.data.entities.LogEntryEntity
import io.github.sds100.keymapper.logging.LogRepository
import io.github.sds100.keymapper.common.state.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class RoomLogRepository(
    private val coroutineScope: CoroutineScope,
    private val dao: LogEntryDao,
) : LogRepository {
    override val log: Flow<State<List<LogEntryEntity>>> = dao.getAll()
        .map { entityList -> State.Data(entityList) }
        .flowOn(Dispatchers.Default)
        .stateIn(
            coroutineScope,
            // save memory by only caching the log when necessary
            SharingStarted.WhileSubscribed(replayExpirationMillis = 1000L),
            State.Loading,
        )

    init {
        dao.getIds()
            .filter { it.size > 1000 }
            .onEach { log ->
                val middleId = log.getOrNull(500) ?: return@onEach
                dao.deleteRowsWithIdLessThan(middleId)
            }.flowOn(Dispatchers.Default)
            .launchIn(coroutineScope)
    }

    override fun deleteAll() {
        coroutineScope.launch(Dispatchers.Default) {
            dao.deleteAll()
        }
    }

    override fun insert(entry: LogEntryEntity) {
        coroutineScope.launch(Dispatchers.Default) {
            dao.insert(entry)
        }
    }

    override suspend fun insertSuspend(entry: LogEntryEntity) {
        dao.insert(entry)
    }
}
