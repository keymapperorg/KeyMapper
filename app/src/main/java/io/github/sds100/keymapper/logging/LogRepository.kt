package io.github.sds100.keymapper.logging

import io.github.sds100.keymapper.data.entities.LogEntryEntity
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 13/05/2021.
 */
interface LogRepository {
    val log: Flow<State<List<LogEntryEntity>>>
    fun insert(entry: LogEntryEntity)
    suspend fun insertSuspend(entry: LogEntryEntity)
    fun deleteAll()
}
