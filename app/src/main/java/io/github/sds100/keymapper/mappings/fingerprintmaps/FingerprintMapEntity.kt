package io.github.sds100.keymapper.mappings.fingerprintmaps

import com.github.salomonbrys.kotson.*
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.Extra

/**
 * Created by sds100 on 08/11/20.
 */

data class FingerprintMapEntity(
    @SerializedName(NAME_VERSION)
    val version: Int = CURRENT_VERSION,

    @SerializedName(NAME_ACTION_LIST)
    val actionList: List<ActionEntity> = listOf(),

    @SerializedName(NAME_CONSTRAINTS)
    val constraintList: List<ConstraintEntity> = listOf(),

    @SerializedName(NAME_CONSTRAINT_MODE)
    val constraintMode: Int = ConstraintEntity.DEFAULT_MODE,

    @SerializedName(NAME_EXTRAS)
    val extras: List<Extra> = listOf(),

    @SerializedName(NAME_FLAGS)
    val flags: Int = 0,

    @SerializedName(NAME_ENABLED)
    val isEnabled: Boolean = true
)  {
    companion object {
        const val ID_SWIPE_DOWN = "swipe_down"
        const val ID_SWIPE_UP = "swipe_up"
        const val ID_SWIPE_LEFT = "swipe_left"
        const val ID_SWIPE_RIGHT = "swipe_right"

        val GESTURES = arrayOf(ID_SWIPE_DOWN, ID_SWIPE_UP, ID_SWIPE_LEFT, ID_SWIPE_RIGHT)

        const val NAME_VERSION = "db_version" // NEVER EVER CHANGE THIS BECAUSE USED TO DETERMINE HOW TO MIGRATE

        //DON'T CHANGE THESE. Used for JSON serialization and parsing.
        private const val NAME_ACTION_LIST = "action_list"
        private const val NAME_EXTRAS = "extras"
        private const val NAME_FLAGS = "flags"
        private const val NAME_ENABLED = "enabled"
        private const val NAME_CONSTRAINTS = "constraints"
        private const val NAME_CONSTRAINT_MODE = "constraint_mode"

        val DESERIALIZER = jsonDeserializer {

            val version by it.json.byNullableInt(NAME_VERSION)

            val actionListJson by it.json.byArray(NAME_ACTION_LIST)
            val actionList = it.context.deserialize<List<ActionEntity>>(actionListJson)

            val extrasJson by it.json.byArray(NAME_EXTRAS)
            val extras = it.context.deserialize<List<Extra>>(extrasJson)

            val constraintsJson by it.json.byArray(NAME_CONSTRAINTS)
            val constraints = it.context.deserialize<List<ConstraintEntity>>(constraintsJson)

            val constraintMode by it.json.byInt(NAME_CONSTRAINT_MODE)

            val flags by it.json.byInt(NAME_FLAGS)

            val isEnabled by it.json.byBool(NAME_ENABLED)

            FingerprintMapEntity(
                version ?: 0, //versioning was added at version 1.
                actionList,
                constraints,
                constraintMode,
                extras,
                flags,
                isEnabled
            )
        }

        const val CURRENT_VERSION = 2

        const val FLAG_VIBRATE = 1
        const val FLAG_SHOW_TOAST = 2
        const val EXTRA_VIBRATION_DURATION = "extra_vibration_duration"
    }
}