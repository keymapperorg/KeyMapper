package io.github.sds100.keymapper.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.sds100.keymapper.data.entities.VariableEntity

@Dao
interface VariableDao {
    companion object {
        const val TABLE_NAME = "variables"
        const val KEY_NAME = "name"
        const val KEY_VALUE = "value"
    }

    @Query("SELECT * FROM $TABLE_NAME")
    suspend fun getAll(): List<VariableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<VariableEntity>)

    @Query("DELETE FROM $TABLE_NAME")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entities: List<VariableEntity>) {
        deleteAll()
        insertAll(entities)
    }
}
