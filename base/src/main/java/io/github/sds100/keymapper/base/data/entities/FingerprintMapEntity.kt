package io.github.sds100.keymapper.base.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.salomonbrys.kotson.byArray
import com.github.salomonbrys.kotson.byBool
import com.github.salomonbrys.kotson.byInt
import com.github.salomonbrys.kotson.byNullableInt
import com.github.salomonbrys.kotson.jsonDeserializer
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.db.dao.FingerprintMapDao

@Entity(tableName = FingerprintMapDao.TABLE_NAME)
data class FingerprintMapEntity(
    @SerializedName(NAME_ID)
    @PrimaryKey
    val id: Int = ID_UNKNOWN,

    @SerializedName(NAME_ACTION_LIST)
    @ColumnInfo(name = FingerprintMapDao.KEY_ACTION_LIST)
    val actionList: List<ActionEntity> = listOf(),

    @SerializedName(NAME_CONSTRAINTS)
    @ColumnInfo(name = FingerprintMapDao.KEY_CONSTRAINT_LIST)
    val constraintList: List<ConstraintEntity> = listOf(),

    @SerializedName(NAME_CONSTRAINT_MODE)
    @ColumnInfo(name = FingerprintMapDao.KEY_CONSTRAINT_MODE)
    val constraintMode: Int = ConstraintEntity.DEFAULT_MODE,

    @SerializedName(NAME_EXTRAS)
    @ColumnInfo(name = FingerprintMapDao.KEY_EXTRAS)
    val extras: List<EntityExtra> = listOf(),

    @SerializedName(NAME_FLAGS)
    @ColumnInfo(name = FingerprintMapDao.KEY_FLAGS)
    val flags: Int = 0,

    @SerializedName(NAME_ENABLED)
    @ColumnInfo(name = FingerprintMapDao.KEY_ENABLED)
    val isEnabled: Boolean = true,
) {
    companion object {
        private const val ID_UNKNOWN = -1
        const val ID_SWIPE_DOWN = 0
        const val ID_SWIPE_UP = 1
        const val ID_SWIPE_LEFT = 2
        const val ID_SWIPE_RIGHT = 3

        const val NAME_ID = "id"

        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        private const val NAME_ACTION_LIST = "action_list"
        private const val NAME_EXTRAS = "extras"
        private const val NAME_FLAGS = "flags"
        private const val NAME_ENABLED = "enabled"
        private const val NAME_CONSTRAINTS = "constraints"
        private const val NAME_CONSTRAINT_MODE = "constraint_mode"

        val DESERIALIZER = jsonDeserializer {

            val id by it.json.byNullableInt(NAME_ID)

            val actionListJson by it.json.byArray(NAME_ACTION_LIST)
            val actionList = it.context.deserialize<List<ActionEntity>>(actionListJson)

            val extrasJson by it.json.byArray(NAME_EXTRAS)
            val extras = it.context.deserialize<List<EntityExtra>>(extrasJson)

            val constraintsJson by it.json.byArray(NAME_CONSTRAINTS)
            val constraints = it.context.deserialize<List<ConstraintEntity>>(constraintsJson)

            val constraintMode by it.json.byInt(NAME_CONSTRAINT_MODE)

            val flags by it.json.byInt(NAME_FLAGS)

            val isEnabled by it.json.byBool(NAME_ENABLED)

            FingerprintMapEntity(
                id = id ?: ID_UNKNOWN,
                actionList,
                constraints,
                constraintMode,
                extras,
                flags,
                isEnabled,
            )
        }

        const val FLAG_VIBRATE = 1
        const val FLAG_SHOW_TOAST = 2
        const val FLAG_MIGRATED_TO_KEY_MAP = 4
        const val EXTRA_VIBRATION_DURATION = "extra_vibration_duration"
    }
}
