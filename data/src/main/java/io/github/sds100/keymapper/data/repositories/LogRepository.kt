package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.data.entities.LogEntryEntity
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    val log: Flow<State<List<LogEntryEntity>>>
    fun insert(entry: LogEntryEntity)
    suspend fun insertSuspend(entry: LogEntryEntity)
    fun deleteAll()
}
