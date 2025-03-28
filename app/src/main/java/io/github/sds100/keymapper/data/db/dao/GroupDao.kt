package io.github.sds100.keymapper.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import io.github.sds100.keymapper.data.entities.GroupEntity
import io.github.sds100.keymapper.data.entities.GroupEntityWithSubGroups
import io.github.sds100.keymapper.data.entities.KeyMapEntitiesWithGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    companion object {
        const val TABLE_NAME = "groups"
        const val KEY_UID = "uid"
        const val KEY_NAME = "name"
        const val KEY_CONSTRAINTS = "constraints"
        const val KEY_CONSTRAINT_MODE = "constraint_mode"
        const val KEY_PARENT_UID = "parent_uid"
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_UID = (:groupUid)")
    fun getKeyMapsByGroup(groupUid: String): Flow<KeyMapEntitiesWithGroup>

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_UID = (:uid)")
    fun getById(uid: String): GroupEntity?

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_UID = (:uid)")
    fun getByIdFlow(uid: String): Flow<GroupEntity?>

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_UID = (:uid)")
    fun getGroupWithSubGroups(uid: String): Flow<GroupEntityWithSubGroups>

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_PARENT_UID = (:uid)")
    fun getGroupsByParent(uid: String?): Flow<List<GroupEntity>>
}
