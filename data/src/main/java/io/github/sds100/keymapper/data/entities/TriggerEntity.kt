package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import androidx.annotation.IntDef
import com.github.salomonbrys.kotson.byArray
import com.github.salomonbrys.kotson.byInt
import com.github.salomonbrys.kotson.byNullableInt
import com.github.salomonbrys.kotson.jsonDeserializer
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class TriggerEntity(
    @SerializedName(NAME_KEYS)
    val keys: List<TriggerKeyEntity> = listOf(),

    @SerializedName(NAME_EXTRAS)
    val extras: List<EntityExtra> = listOf(),

    @Mode
    @SerializedName(NAME_MODE)
    val mode: Int = DEFAULT_TRIGGER_MODE,

    @SerializedName(NAME_FLAGS)
    val flags: Int = 0,
) : Parcelable {

    companion object {
        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_KEYS = "keys"
        const val NAME_EXTRAS = "extras"
        const val NAME_MODE = "mode"
        const val NAME_FLAGS = "flags"

        const val PARALLEL = 0
        const val SEQUENCE = 1
        const val UNDEFINED = 2

        // DON'T CHANGE THESE AND THEY MUST BE POWERS OF 2!!
        const val TRIGGER_FLAG_VIBRATE = 1
        const val TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION = 2
        const val TRIGGER_FLAG_SCREEN_OFF_TRIGGERS = 4
        const val TRIGGER_FLAG_FROM_OTHER_APPS = 8
        const val TRIGGER_FLAG_SHOW_TOAST = 16

        const val DEFAULT_TRIGGER_MODE = UNDEFINED

        const val EXTRA_SEQUENCE_TRIGGER_TIMEOUT = "extra_sequence_trigger_timeout"
        const val EXTRA_LONG_PRESS_DELAY = "extra_long_press_delay"
        const val EXTRA_DOUBLE_PRESS_DELAY = "extra_double_press_timeout"
        const val EXTRA_VIBRATION_DURATION = "extra_vibration_duration"

        val DESERIALIZER = jsonDeserializer {
            val triggerKeysJsonArray by it.json.byArray(NAME_KEYS)
            val keys = it.context.deserialize<List<TriggerKeyEntity>>(triggerKeysJsonArray)

            val extrasJsonArray by it.json.byArray(NAME_EXTRAS)
            val extraList = it.context.deserialize<List<EntityExtra>>(extrasJsonArray) ?: listOf()

            val mode by it.json.byInt(NAME_MODE)

            val flags by it.json.byNullableInt(NAME_FLAGS)

            TriggerEntity(keys, extraList, mode, flags ?: 0)
        }
    }

    @IntDef(value = [PARALLEL, SEQUENCE, UNDEFINED])
    annotation class Mode
}
