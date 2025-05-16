package io.github.sds100.keymapper.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.sds100.keymapper.data.entities.LogEntryEntity
import kotlinx.coroutines.flow.Flow



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

    @Query("SELECT $KEY_ID FROM $TABLE_NAME ORDER BY $KEY_TIME ASC")
    fun getIds(): Flow<List<Int>>

    @Query("DELETE FROM $TABLE_NAME")
    suspend fun deleteAll()

    @Query("DELETE FROM $TABLE_NAME WHERE $KEY_ID < :id")
    suspend fun deleteRowsWithIdLessThan(id: Int)

    @Insert
    suspend fun insert(vararg entry: LogEntryEntity)
}
