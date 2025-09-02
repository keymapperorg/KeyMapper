package io.github.sds100.keymapper.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.sds100.keymapper.data.entities.FingerprintMapEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FingerprintMapDao {
    companion object {
        const val TABLE_NAME = "fingerprintmaps"
        const val KEY_ID = "id"
        const val KEY_FLAGS = "flags"
        const val KEY_ENABLED = "is_enabled"
        const val KEY_ACTION_LIST = "action_list"
        const val KEY_CONSTRAINT_LIST = "constraint_list"
        const val KEY_CONSTRAINT_MODE = "constraint_mode"
        const val KEY_EXTRAS = "extras"
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_ID = (:id)")
    suspend fun getById(id: Int): FingerprintMapEntity?

    @Query("SELECT * FROM $TABLE_NAME")
    fun getAll(): Flow<List<FingerprintMapEntity>>

    @Query("UPDATE $TABLE_NAME SET $KEY_ENABLED=1 WHERE $KEY_ID in (:id)")
    suspend fun enableById(vararg id: Int)

    @Query("UPDATE $TABLE_NAME SET $KEY_ENABLED=0 WHERE $KEY_ID in (:id)")
    suspend fun disableById(vararg id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg fingerprintMap: FingerprintMapEntity)

    @Update
    suspend fun update(vararg fingerprintMap: FingerprintMapEntity)
}
