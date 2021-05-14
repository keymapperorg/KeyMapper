package io.github.sds100.keymapper.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.sds100.keymapper.data.entities.LogEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 18/02/20.
 */

@Dao
interface LogEntryDao {
    companion object {
        const val TABLE_NAME = "log"
        const val KEY_ID = "id"
        const val KEY_TIME = "time"
        const val KEY_SEVERITY = "severity"
        const val KEY_MESSAGE = "message"
    }

    @Query("SELECT * FROM $TABLE_NAME ORDER BY $KEY_TIME ASC")
    fun getAll(): Flow<List<LogEntryEntity>>

    @Query("DELETE FROM $TABLE_NAME")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(vararg entry: LogEntryEntity)
}