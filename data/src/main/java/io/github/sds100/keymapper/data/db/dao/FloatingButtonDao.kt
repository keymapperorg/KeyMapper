package io.github.sds100.keymapper.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.sds100.keymapper.data.entities.FloatingButtonEntity
import io.github.sds100.keymapper.data.entities.FloatingButtonEntityWithLayout
import kotlinx.coroutines.flow.Flow

@Dao
interface FloatingButtonDao {
    companion object {
        const val TABLE_NAME = "floating_buttons"
        const val KEY_UID = "uid"
        const val KEY_LAYOUT_UID = "layout_uid"
        const val KEY_TEXT = "text"
        const val KEY_BUTTON_SIZE = "button_size"
        const val KEY_X = "x"
        const val KEY_Y = "y"
        const val KEY_ORIENTATION = "orientation"
        const val KEY_DISPLAY_WIDTH = "display_width"
        const val KEY_DISPLAY_HEIGHT = "display_height"
        const val KEY_BORDER_OPACITY = "border_opacity"
        const val KEY_BACKGROUND_OPACITY = "background_opacity"
        const val KEY_SHOW_OVER_STATUS_BAR = "show_over_status_bar"
        const val KEY_SHOW_OVER_INPUT_METHOD = "show_over_input_method"
        const val KEY_POSITION_LOCKED = "position_locked"
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_UID = (:uid)")
    suspend fun getByUid(uid: String): FloatingButtonEntity?

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_UID = (:uid)")
    suspend fun getByUidWithLayout(uid: String): FloatingButtonEntityWithLayout?

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME")
    fun getAll(): Flow<List<FloatingButtonEntityWithLayout>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg button: FloatingButtonEntity)

    @Query("DELETE FROM $TABLE_NAME")
    suspend fun deleteAll()

    @Query("DELETE FROM $TABLE_NAME WHERE $KEY_UID in (:uid)")
    suspend fun deleteByUid(vararg uid: String)

    @Update
    suspend fun update(vararg button: FloatingButtonEntity)
}
