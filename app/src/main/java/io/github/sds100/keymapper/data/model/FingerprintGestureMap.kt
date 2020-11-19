package io.github.sds100.keymapper.data.model

import com.github.salomonbrys.kotson.*
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 08/11/20.
 */
data class FingerprintGestureMap(
    val action: Action? = null,
    val constraintList: List<Constraint> = listOf(),
    val extras: List<Extra> = listOf(),
    val flags: Int = 0,
    val isEnabled: Boolean = true
) {
    companion object {
        //DON'T CHANGE THESE. Used for JSON serialization and parsing.
        private const val NAME_ACTION = "action"
        private const val NAME_EXTRAS = "extras"
        private const val NAME_FLAGS = "flags"
        private const val NAME_ENABLED = "enabled"
        private const val NAME_CONSTRAINTS = "constraints"

        val DESERIALIZER = jsonDeserializer {

            val actionJson by it.json.byObject(NAME_ACTION)
            val action = it.context.deserialize<Action>(actionJson)

            val extrasJson by it.json.byArray(NAME_EXTRAS)
            val extras = it.context.deserialize<List<Extra>>(extrasJson)

            val constraintsJson by it.json.byArray(NAME_CONSTRAINTS)
            val constraints = it.context.deserialize<List<Constraint>>(constraintsJson)

            val flags by it.json.byInt(NAME_FLAGS)

            val isEnabled by it.json.byBool(NAME_ENABLED)

            FingerprintGestureMap(action, constraints, extras, flags, isEnabled)
        }

        const val FLAG_VIBRATE = 1
        const val EXTRA_VIBRATION_DURATION = "extra_vibration_duration"

        val FLAG_LABEL_MAP = mapOf(
            FLAG_VIBRATE to R.string.flag_vibrate
        )
    }

    fun clone(action: Action? = this.action,
              constraintList: List<Constraint> = this.constraintList,
              extras: List<Extra> = this.extras,
              flags: Int = this.flags,
              isEnabled: Boolean = this.isEnabled) =
        FingerprintGestureMap(
            action,
            constraintList,
            extras,
            flags,
            isEnabled
        )
}