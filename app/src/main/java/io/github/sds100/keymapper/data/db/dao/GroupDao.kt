package io.github.sds100.keymapper.data.db.dao

import androidx.room.Dao

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
}
