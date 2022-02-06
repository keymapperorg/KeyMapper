package io.github.sds100.keymapper.data.db.dao

import androidx.room.*
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 18/02/20.
 */

@Dao
interface KeyMapDao {
    companion object {
        const val TABLE_NAME = "keymaps"
        const val KEY_ID = "id"
        const val KEY_FLAGS = "flags"
        const val KEY_ENABLED = "is_enabled"
        const val KEY_TRIGGER = "trigger"
        const val KEY_ACTION_LIST = "action_list"
        const val KEY_CONSTRAINT_LIST = "constraint_list"
        const val KEY_CONSTRAINT_MODE = "constraint_mode"
        const val KEY_FOLDER_NAME = "folder_name"
        const val KEY_UID = "uid"
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_ID = (:id)")
    suspend fun getById(id: Long): KeyMapEntity

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_UID = (:uid)")
    suspend fun getByUid(uid: String): KeyMapEntity?

    @Query("SELECT * FROM $TABLE_NAME")
    fun getAll(): Flow<List<KeyMapEntity>>

    @Query("UPDATE $TABLE_NAME SET $KEY_ENABLED=0")
    suspend fun disableAll()

    @Query("UPDATE $TABLE_NAME SET $KEY_ENABLED=1")
    suspend fun enableAll()

    @Query("UPDATE $TABLE_NAME SET $KEY_ENABLED=1 WHERE $KEY_UID in (:uid)")
    suspend fun enableKeymapByUid(vararg uid: String)

    @Query("UPDATE $TABLE_NAME SET $KEY_ENABLED=0 WHERE $KEY_UID in (:uid)")
    suspend fun disableKeymapByUid(vararg uid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg keyMap: KeyMapEntity)

    @Delete
    suspend fun delete(vararg keyMap: KeyMapEntity)

    @Query("DELETE FROM $TABLE_NAME")
    suspend fun deleteAll()

    @Query("DELETE FROM $TABLE_NAME WHERE $KEY_UID in (:uid)")
    suspend fun deleteById(vararg uid: String)

    @Update
    suspend fun update(vararg keyMap: KeyMapEntity)
}