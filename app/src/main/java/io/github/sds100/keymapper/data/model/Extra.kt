package io.github.sds100.keymapper.data.model

import androidx.annotation.StringDef
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.R
import java.io.Serializable

/**
 * Created by sds100 on 26/01/2019.
 */

@StringDef(value = [
    Extra.EXTRA_PACKAGE_NAME,
    Extra.EXTRA_SHORTCUT_TITLE,
    Extra.EXTRA_STREAM_TYPE,
    Extra.EXTRA_LENS,
    Extra.EXTRA_RINGER_MODE,
    Extra.EXTRA_SEQUENCE_TRIGGER_TIMEOUT,
    Extra.EXTRA_LONG_PRESS_DELAY,
    Extra.EXTRA_DOUBLE_PRESS_DELAY,
    Extra.EXTRA_HOLD_DOWN_DELAY,
    Extra.EXTRA_REPEAT_DELAY,
    Extra.EXTRA_VIBRATION_DURATION,
    Extra.EXTRA_BT_ADDRESS
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

        //DON'T CHANGE THESE IDs!!!!
        //Actions
        const val EXTRA_SHORTCUT_TITLE = "extra_title"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_STREAM_TYPE = "extra_stream_type"
        const val EXTRA_LENS = "extra_flash"
        const val EXTRA_RINGER_MODE = "extra_ringer_mode"

        //Trigger
        const val EXTRA_SEQUENCE_TRIGGER_TIMEOUT = "extra_sequence_trigger_timeout"
        const val EXTRA_LONG_PRESS_DELAY = "extra_long_press_delay"
        const val EXTRA_DOUBLE_PRESS_DELAY = "extra_double_press_timeout"
        const val EXTRA_HOLD_DOWN_DELAY = "extra_hold_down_until_repeat_delay"
        const val EXTRA_REPEAT_DELAY = "extra_repeat_delay"
        const val EXTRA_VIBRATION_DURATION = "extra_vibration_duration"

        const val EXTRA_BT_ADDRESS = "extra_bluetooth_device_address"
        const val EXTRA_BT_NAME = "extra_bluetooth_device_name"

        val TRIGGER_EXTRAS = arrayOf(
            EXTRA_SEQUENCE_TRIGGER_TIMEOUT,
            EXTRA_LONG_PRESS_DELAY,
            EXTRA_DOUBLE_PRESS_DELAY,
            EXTRA_VIBRATION_DURATION
        )

        val ACTION_EXTRAS = arrayOf(
            EXTRA_HOLD_DOWN_DELAY,
            EXTRA_REPEAT_DELAY
        )

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
            EXTRA_HOLD_DOWN_DELAY to R.string.extra_label_hold_down_delay_label,
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