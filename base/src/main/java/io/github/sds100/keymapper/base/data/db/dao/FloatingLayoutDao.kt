package io.github.sds100.keymapper.base.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.sds100.keymapper.data.entities.FloatingLayoutEntity
import io.github.sds100.keymapper.data.entities.FloatingLayoutEntityWithButtons
import kotlinx.coroutines.flow.Flow

@Dao
interface FloatingLayoutDao {
    companion object {
        const val TABLE_NAME = "floating_layouts"
        const val KEY_UID = "uid"
        const val KEY_NAME = "name"
    }

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_UID = (:uid)")
    suspend fun getByUidWithButtons(uid: String): FloatingLayoutEntityWithButtons?

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_UID = (:uid)")
    fun getByUidWithButtonsFlow(uid: String): Flow<FloatingLayoutEntityWithButtons?>

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME")
    fun getAllWithButtons(): Flow<List<FloatingLayoutEntityWithButtons>>

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_UID = (:uid)")
    suspend fun getByUid(uid: String): FloatingLayoutEntity?

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_UID = (:uid)")
    fun getByUidFlow(uid: String): Flow<FloatingLayoutEntity?>

    @Query("SELECT * FROM $TABLE_NAME")
    fun getAll(): Flow<List<FloatingLayoutEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(vararg layout: FloatingLayoutEntity)

    @Delete
    suspend fun delete(vararg layout: FloatingLayoutEntity)

    @Query("DELETE FROM $TABLE_NAME")
    suspend fun deleteAll()

    @Query("DELETE FROM $TABLE_NAME WHERE $KEY_UID in (:uid)")
    suspend fun deleteByUid(vararg uid: String)

    @Update
    suspend fun update(vararg layout: FloatingLayoutEntity)

    @Query("SELECT COUNT(*) FROM $TABLE_NAME")
    suspend fun count(): Int
}
