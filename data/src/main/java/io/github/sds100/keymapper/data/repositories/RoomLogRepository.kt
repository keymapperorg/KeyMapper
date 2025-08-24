package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.db.dao.LogEntryDao
import io.github.sds100.keymapper.data.entities.LogEntryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomLogRepository @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val dao: LogEntryDao,
) : LogRepository {
    override val log: Flow<List<LogEntryEntity>> = dao.getAll()
        .flowOn(Dispatchers.Default)

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
