package io.github.sds100.keymapper.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.sds100.keymapper.data.entities.AccessibilityNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccessibilityNodeDao {
    companion object {
        const val TABLE_NAME = "accessibility_nodes"
        const val KEY_ID = "id"
        const val KEY_PACKAGE_NAME = "package_name"
        const val KEY_TEXT = "text"
        const val KEY_CONTENT_DESCRIPTION = "content_description"
        const val KEY_CLASS_NAME = "class_name"
        const val KEY_VIEW_RESOURCE_ID = "view_resource_id"
        const val KEY_UNIQUE_ID = "unique_id"
        const val KEY_ACTIONS = "actions"
        const val KEY_INTERACTED = "interacted"
        const val KEY_TOOLTIP = "tooltip"
        const val KEY_HINT = "hint"
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_ID = (:id)")
    suspend fun getById(id: Long): AccessibilityNodeEntity?

    @Query("SELECT * FROM $TABLE_NAME")
    fun getAll(): Flow<List<AccessibilityNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(vararg node: AccessibilityNodeEntity)

    @Query("DELETE FROM $TABLE_NAME")
    suspend fun deleteAll()
}
