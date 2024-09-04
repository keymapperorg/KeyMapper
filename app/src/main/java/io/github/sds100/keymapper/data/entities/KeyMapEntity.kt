package io.github.sds100.keymapper.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.salomonbrys.kotson.byArray
import com.github.salomonbrys.kotson.byBool
import com.github.salomonbrys.kotson.byInt
import com.github.salomonbrys.kotson.byNullableString
import com.github.salomonbrys.kotson.byObject
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import java.util.UUID

/**
 * Created by sds100 on 12/07/2018.
 */

@Entity(tableName = KeyMapDao.TABLE_NAME)
data class KeyMapEntity(
    @SerializedName(NAME_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    @SerializedName(NAME_TRIGGER)
    @ColumnInfo(name = KeyMapDao.KEY_TRIGGER)
    val trigger: TriggerEntity = TriggerEntity(),

    @SerializedName(NAME_ACTION_LIST)
    @ColumnInfo(name = KeyMapDao.KEY_ACTION_LIST)
    val actionList: List<ActionEntity> = listOf(),

    @SerializedName(NAME_CONSTRAINT_LIST)
    @ColumnInfo(name = KeyMapDao.KEY_CONSTRAINT_LIST)
    val constraintList: List<ConstraintEntity> = listOf(),

    @SerializedName(NAME_CONSTRAINT_MODE)
    @ColumnInfo(name = KeyMapDao.KEY_CONSTRAINT_MODE)
    val constraintMode: Int = ConstraintEntity.DEFAULT_MODE,

    /**
     * Flags are stored as bits.
     */
    @SerializedName(NAME_FLAGS)
    @ColumnInfo(name = KeyMapDao.KEY_FLAGS)
    val flags: Int = 0,

    @SerializedName(NAME_FOLDER_NAME)
    @ColumnInfo(name = KeyMapDao.KEY_FOLDER_NAME)
    val folderName: String? = null,

    @SerializedName(NAME_IS_ENABLED)
    @ColumnInfo(name = KeyMapDao.KEY_ENABLED)
    val isEnabled: Boolean = true,

    @SerializedName(NAME_UID)
    @ColumnInfo(name = KeyMapDao.KEY_UID)
    val uid: String = UUID.randomUUID().toString(),
) {
    companion object {

        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_ID = "id"
        const val NAME_TRIGGER = "trigger"
        const val NAME_ACTION_LIST = "actionList"
        const val NAME_CONSTRAINT_LIST = "constraintList"
        const val NAME_CONSTRAINT_MODE = "constraintMode"
        const val NAME_FLAGS = "flags"
        const val NAME_FOLDER_NAME = "folderName"
        const val NAME_IS_ENABLED = "isEnabled"
        const val NAME_UID = "uid"

        val DESERIALIZER = jsonDeserializer {
            val actionListJsonArray by it.json.byArray(NAME_ACTION_LIST)
            val actionList = it.context.deserialize<List<ActionEntity>>(actionListJsonArray)

            val triggerJsonObject by it.json.byObject(NAME_TRIGGER)
            val trigger = it.context.deserialize<TriggerEntity>(triggerJsonObject)

            val constraintListJsonArray by it.json.byArray(NAME_CONSTRAINT_LIST)
            val constraintList =
                it.context.deserialize<List<ConstraintEntity>>(constraintListJsonArray)

            val constraintMode by it.json.byInt(NAME_CONSTRAINT_MODE)
            val flags by it.json.byInt(NAME_FLAGS)
            val folderName by it.json.byNullableString(NAME_FOLDER_NAME)
            val isEnabled by it.json.byBool(NAME_IS_ENABLED)
            val uid by it.json.byString(NAME_UID) { UUID.randomUUID().toString() }

            KeyMapEntity(
                0,
                trigger,
                actionList,
                constraintList,
                constraintMode,
                flags,
                folderName,
                isEnabled,
                uid,
            )
        }
    }
}
