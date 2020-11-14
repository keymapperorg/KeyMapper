package io.github.sds100.keymapper.data.model

import com.github.salomonbrys.kotson.*

/**
 * Created by sds100 on 08/11/20.
 */
data class FingerprintGestureMap(
    val action: Action? = null,
    val extras: List<Extra> = listOf(),
    val flags: Int = 0,
    val isEnabled: Boolean = true
) {
    companion object {
        //DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_ACTION = "action"
        const val NAME_EXTRAS = "extras"
        const val NAME_FLAGS = "flags"
        const val NAME_ENABLED = "enabled"

        val DESERIALIZER = jsonDeserializer {

            val actionJson by it.json.byObject(NAME_ACTION)
            val action = it.context.deserialize<Action>(actionJson)

            val extrasJson by it.json.byArray(NAME_EXTRAS)
            val extras = it.context.deserialize<List<Extra>>(extrasJson)

            val flags by it.json.byInt(NAME_FLAGS)

            val isEnabled by it.json.byBool(NAME_ENABLED)

            FingerprintGestureMap(action, extras, flags, isEnabled)
        }
    }
}