package io.github.sds100.keymapper.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.db.dao.KeyMapDao

/**
 * Created by sds100 on 12/07/2018.
 */

@Entity(tableName = KeyMapDao.TABLE_NAME)
class KeyMap(
    @SerializedName(NAME_ID)
    @PrimaryKey(autoGenerate = true)
    var id: Long,

    @SerializedName(NAME_TRIGGER)
    @ColumnInfo(name = KeyMapDao.KEY_TRIGGER)
    var trigger: Trigger = Trigger(),

    @SerializedName(NAME_ACTION_LIST)
    @ColumnInfo(name = KeyMapDao.KEY_ACTION_LIST)
    var actionList: List<Action> = listOf(),

    @SerializedName(NAME_CONSTRAINT_LIST)
    @ColumnInfo(name = KeyMapDao.KEY_CONSTRAINT_LIST)
    var constraintList: List<Constraint> = listOf(),

    @ConstraintMode
    @SerializedName(NAME_CONSTRAINT_MODE)
    @ColumnInfo(name = KeyMapDao.KEY_CONSTRAINT_MODE)
    var constraintMode: Int = Constraint.DEFAULT_MODE,

    @SerializedName(NAME_FLAGS)
    @ColumnInfo(name = KeyMapDao.KEY_FLAGS)
    /**
     * Flags are stored as bits.
     */
    var flags: Int = 0,

    @SerializedName(NAME_FOLDER_NAME)
    @ColumnInfo(name = KeyMapDao.KEY_FOLDER_NAME)
    var folderName: String? = null,

    @SerializedName(NAME_IS_ENABLED)
    @ColumnInfo(name = KeyMapDao.KEY_ENABLED)
    var isEnabled: Boolean = true
) {
    companion object {

        //DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_ID = "id"
        const val NAME_TRIGGER = "trigger"
        const val NAME_ACTION_LIST = "actionList"
        const val NAME_CONSTRAINT_LIST = "constraintList"
        const val NAME_CONSTRAINT_MODE = "constraintMode"
        const val NAME_FLAGS = "flags"
        const val NAME_FOLDER_NAME = "folderName"
        const val NAME_IS_ENABLED = "isEnabled"
    }

    override fun hashCode() = id.toInt()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyMap

        if (id != other.id) return false

        return true
    }

    fun clone(): KeyMap = KeyMap(0, trigger, actionList, constraintList, constraintMode, flags, folderName, isEnabled)
}