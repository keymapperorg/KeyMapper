package io.github.sds100.keymapper.base.logging

import io.github.sds100.keymapper.data.entities.LogEntryEntity
import io.github.sds100.keymapper.common.util.state.State
import kotlinx.coroutines.flow.Flow


interface LogRepository {
    val log: Flow<State<List<LogEntryEntity>>>
    fun insert(entry: LogEntryEntity)
    suspend fun insertSuspend(entry: LogEntryEntity)
    fun deleteAll()
}
