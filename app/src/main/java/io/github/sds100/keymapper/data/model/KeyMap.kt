package io.github.sds100.keymapper.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.salomonbrys.kotson.*
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import kotlinx.android.parcel.Parcelize

/**
 * Created by sds100 on 12/07/2018.
 */

@Parcelize
@Entity(tableName = KeyMapDao.TABLE_NAME)
data class KeyMap(
    @SerializedName(NAME_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    @SerializedName(NAME_TRIGGER)
    @ColumnInfo(name = KeyMapDao.KEY_TRIGGER)
    val trigger: Trigger = Trigger(),

    @SerializedName(NAME_ACTION_LIST)
    @ColumnInfo(name = KeyMapDao.KEY_ACTION_LIST)
    val actionList: List<Action> = listOf(),

    @SerializedName(NAME_CONSTRAINT_LIST)
    @ColumnInfo(name = KeyMapDao.KEY_CONSTRAINT_LIST)
    val constraintList: List<Constraint> = listOf(),

    @ConstraintMode
    @SerializedName(NAME_CONSTRAINT_MODE)
    @ColumnInfo(name = KeyMapDao.KEY_CONSTRAINT_MODE)
    val constraintMode: Int = Constraint.DEFAULT_MODE,

    @SerializedName(NAME_FLAGS)
    @ColumnInfo(name = KeyMapDao.KEY_FLAGS)
    /**
     * Flags are stored as bits.
     */
    val flags: Int = 0,

    @SerializedName(NAME_FOLDER_NAME)
    @ColumnInfo(name = KeyMapDao.KEY_FOLDER_NAME)
    val folderName: String? = null,

    @SerializedName(NAME_IS_ENABLED)
    @ColumnInfo(name = KeyMapDao.KEY_ENABLED)
    val isEnabled: Boolean = true
) : Parcelable {
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

        val DESERIALIZER = jsonDeserializer {
            val actionListJsonArray by it.json.byArray(NAME_ACTION_LIST)
            val actionList = it.context.deserialize<List<Action>>(actionListJsonArray)

            val triggerJsonObject by it.json.byObject(NAME_TRIGGER)
            val trigger = it.context.deserialize<Trigger>(triggerJsonObject)

            val constraintListJsonArray by it.json.byArray(NAME_CONSTRAINT_LIST)
            val constraintList = it.context.deserialize<List<Constraint>>(constraintListJsonArray)

            val constraintMode by it.json.byInt(NAME_CONSTRAINT_MODE)
            val flags by it.json.byInt(NAME_FLAGS)
            val folderName by it.json.byNullableString(NAME_FOLDER_NAME)
            val isEnabled by it.json.byBool(NAME_IS_ENABLED)

            KeyMap(
                0,
                trigger,
                actionList,
                constraintList,
                constraintMode,
                flags,
                folderName,
                isEnabled
            )
        }
    }

    override fun hashCode() = id.toInt()
    override fun equals(other: Any?): Boolean {
        return (other as KeyMap?)?.id == this.id
    }
}