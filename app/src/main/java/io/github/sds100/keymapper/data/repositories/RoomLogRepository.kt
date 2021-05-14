package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.db.dao.LogEntryDao
import io.github.sds100.keymapper.data.entities.LogEntryEntity
import io.github.sds100.keymapper.logging.LogRepository
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 13/05/2021.
 */
class RoomLogRepository(
    private val coroutineScope: CoroutineScope,
    private val dao: LogEntryDao
) : LogRepository {
    override val log: Flow<State<List<LogEntryEntity>>> = dao.getAll()
        .map { entityList ->
            State.Data(entityList)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            coroutineScope,
            SharingStarted.Lazily,
            State.Loading
        )

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