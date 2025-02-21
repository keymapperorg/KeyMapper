package io.github.sds100.keymapper.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.sds100.keymapper.data.entities.FloatingButtonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FloatingButtonDao {
    companion object {
        const val TABLE_NAME = "floating_buttons"
        const val KEY_UID = "uid"
        const val KEY_LAYOUT_UID = "layout_uid"
        const val KEY_NAME = "name"
        const val KEY_ICON = "icon"
        const val KEY_BUTTON_SIZE = "button_size"
        const val KEY_X = "x"
        const val KEY_Y = "y"
        const val KEY_ORIENTATION = "orientation"
        const val KEY_DISPLAY_WIDTH = "display_width"
        const val KEY_DISPLAY_HEIGHT = "display_height"
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_UID = (:uid)")
    suspend fun getByUid(uid: String): FloatingButtonEntity?

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_LAYOUT_UID = (:layoutUid)")
    suspend fun getButtonsForLayout(layoutUid: String): List<FloatingButtonEntity>

    @Query("SELECT * FROM $TABLE_NAME")
    fun getAll(): Flow<List<FloatingButtonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg button: FloatingButtonEntity)

    @Delete
    suspend fun delete(vararg button: FloatingButtonEntity)

    @Query("DELETE FROM $TABLE_NAME")
    suspend fun deleteAll()

    @Query("DELETE FROM $TABLE_NAME WHERE $KEY_UID in (:uid)")
    suspend fun deleteByUid(vararg uid: String)

    @Update
    suspend fun update(vararg button: FloatingButtonEntity)
}
