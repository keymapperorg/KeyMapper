package io.github.sds100.keymapper.data.model

import androidx.annotation.StringDef
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Action.Companion.EXTRA_HOLD_DOWN_DELAY
import io.github.sds100.keymapper.data.model.Action.Companion.EXTRA_LENS
import io.github.sds100.keymapper.data.model.Action.Companion.EXTRA_PACKAGE_NAME
import io.github.sds100.keymapper.data.model.Action.Companion.EXTRA_REPEAT_DELAY
import io.github.sds100.keymapper.data.model.Action.Companion.EXTRA_RINGER_MODE
import io.github.sds100.keymapper.data.model.Action.Companion.EXTRA_SHORTCUT_TITLE
import io.github.sds100.keymapper.data.model.Action.Companion.EXTRA_STREAM_TYPE
import io.github.sds100.keymapper.data.model.Constraint.Companion.EXTRA_BT_ADDRESS
import io.github.sds100.keymapper.data.model.Constraint.Companion.EXTRA_BT_NAME
import io.github.sds100.keymapper.data.model.Trigger.Companion.EXTRA_DOUBLE_PRESS_DELAY
import io.github.sds100.keymapper.data.model.Trigger.Companion.EXTRA_LONG_PRESS_DELAY
import io.github.sds100.keymapper.data.model.Trigger.Companion.EXTRA_SEQUENCE_TRIGGER_TIMEOUT
import io.github.sds100.keymapper.data.model.Trigger.Companion.EXTRA_VIBRATION_DURATION
import java.io.Serializable

/**
 * Created by sds100 on 26/01/2019.
 */

@StringDef(value = [
    EXTRA_PACKAGE_NAME,
    EXTRA_SHORTCUT_TITLE,
    EXTRA_STREAM_TYPE,
    EXTRA_LENS,
    EXTRA_RINGER_MODE,
    EXTRA_SEQUENCE_TRIGGER_TIMEOUT,
    EXTRA_LONG_PRESS_DELAY,
    EXTRA_DOUBLE_PRESS_DELAY,
    EXTRA_HOLD_DOWN_DELAY,
    EXTRA_REPEAT_DELAY,
    EXTRA_VIBRATION_DURATION,
    EXTRA_BT_ADDRESS,
    EXTRA_BT_NAME
])
annotation class ExtraId

data class Extra(@ExtraId
                 @SerializedName(NAME_ID)
                 val id: String,

                 @SerializedName(NAME_DATA)
                 val data: String) : Serializable {
    companion object {

        //DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_ID = "id"
        const val NAME_DATA = "data"

        val EXTRA_MAX_VALUES = mapOf(
            EXTRA_SEQUENCE_TRIGGER_TIMEOUT to R.integer.sequence_trigger_timeout_max,
            EXTRA_LONG_PRESS_DELAY to R.integer.long_press_delay_max,
            EXTRA_DOUBLE_PRESS_DELAY to R.integer.double_press_delay_max,
            EXTRA_HOLD_DOWN_DELAY to R.integer.hold_down_delay_max,
            EXTRA_REPEAT_DELAY to R.integer.repeat_delay_max,
            EXTRA_VIBRATION_DURATION to R.integer.vibrate_duration_max
        )

        val EXTRA_MIN_VALUES = mapOf(
            EXTRA_SEQUENCE_TRIGGER_TIMEOUT to R.integer.sequence_trigger_timeout_min,
            EXTRA_LONG_PRESS_DELAY to R.integer.long_press_delay_min,
            EXTRA_DOUBLE_PRESS_DELAY to R.integer.double_press_delay_min,
            EXTRA_HOLD_DOWN_DELAY to R.integer.hold_down_delay_min,
            EXTRA_REPEAT_DELAY to R.integer.repeat_delay_min,
            EXTRA_VIBRATION_DURATION to R.integer.vibrate_duration_min
        )

        val EXTRA_STEP_SIZE_VALUES = mapOf(
            EXTRA_SEQUENCE_TRIGGER_TIMEOUT to R.integer.sequence_trigger_timeout_step_size,
            EXTRA_LONG_PRESS_DELAY to R.integer.long_press_delay_step_size,
            EXTRA_DOUBLE_PRESS_DELAY to R.integer.double_press_delay_step_size,
            EXTRA_HOLD_DOWN_DELAY to R.integer.hold_down_delay_step_size,
            EXTRA_REPEAT_DELAY to R.integer.repeat_delay_step_size,
            EXTRA_VIBRATION_DURATION to R.integer.vibrate_duration_step_size
        )

        val EXTRA_LABELS = mapOf(
            EXTRA_SEQUENCE_TRIGGER_TIMEOUT to R.string.extra_label_sequence_trigger_timeout,
            EXTRA_LONG_PRESS_DELAY to R.string.extra_label_long_press_delay_timeout,
            EXTRA_DOUBLE_PRESS_DELAY to R.string.extra_label_double_press_delay_timeout,
            EXTRA_HOLD_DOWN_DELAY to R.string.extra_label_hold_down_delay,
            EXTRA_REPEAT_DELAY to R.string.extra_label_repeat_delay,
            EXTRA_VIBRATION_DURATION to R.string.extra_label_vibration_duration
        )

        val EXTRA_DEFAULTS = mapOf(
            EXTRA_SEQUENCE_TRIGGER_TIMEOUT to R.integer.default_value_sequence_trigger_timeout,
            EXTRA_LONG_PRESS_DELAY to R.integer.default_value_long_press_delay,
            EXTRA_DOUBLE_PRESS_DELAY to R.integer.default_value_double_press_delay,
            EXTRA_HOLD_DOWN_DELAY to R.integer.default_value_hold_down_delay,
            EXTRA_REPEAT_DELAY to R.integer.default_value_repeat_delay,
            EXTRA_VIBRATION_DURATION to R.integer.default_value_vibrate_duration
        )
    }
}

fun List<Extra>.putExtraData(id: String, data: String): List<Extra> {
    return this.toMutableList().apply {
        removeAll { it.id == id }
        add(Extra(id, data))
    }
}
