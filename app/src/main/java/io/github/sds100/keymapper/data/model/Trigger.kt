package io.github.sds100.keymapper.data.model

import android.view.KeyEvent
import androidx.annotation.IntDef
import com.github.salomonbrys.kotson.*
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.KeyEventUtils
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * @property [keys] The key codes which will trigger the action
 */
class Trigger(
    @SerializedName(NAME_KEYS)
    val keys: List<Key> = listOf(),

    @SerializedName(NAME_EXTRAS)
    val extras: List<Extra> = listOf(),

    @Mode
    @SerializedName(NAME_MODE)
    val mode: Int = DEFAULT_TRIGGER_MODE,

    @SerializedName(NAME_FLAGS)
    val flags: Int = 0
) {

    companion object {
        //DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_KEYS = "keys"
        const val NAME_EXTRAS = "extras"
        const val NAME_MODE = "mode"
        const val NAME_FLAGS = "flags"

        const val PARALLEL = 0
        const val SEQUENCE = 1
        const val UNDEFINED = 2

        //DON'T CHANGE THESE AND THEY MUST BE POWERS OF 2!!
        const val TRIGGER_FLAG_VIBRATE = 1
        const val TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION = 2
        const val TRIGGER_FLAG_SCREEN_OFF_TRIGGERS = 4

        val TRIGGER_FLAG_LABEL_MAP = mapOf(
            TRIGGER_FLAG_VIBRATE to R.string.flag_vibrate,
            TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION to R.string.flag_long_press_double_vibration,
            TRIGGER_FLAG_SCREEN_OFF_TRIGGERS to R.string.flag_detect_triggers_screen_off,
        )

        const val DEFAULT_TRIGGER_MODE = UNDEFINED
        const val DEFAULT_FLAGS = 0

        const val UNDETERMINED = -1
        const val SHORT_PRESS = 0
        const val LONG_PRESS = 1
        const val DOUBLE_PRESS = 2

        const val EXTRA_SEQUENCE_TRIGGER_TIMEOUT = "extra_sequence_trigger_timeout"
        const val EXTRA_LONG_PRESS_DELAY = "extra_long_press_delay"
        const val EXTRA_DOUBLE_PRESS_DELAY = "extra_double_press_timeout"
        const val EXTRA_VIBRATION_DURATION = "extra_vibration_duration"

        val EXTRAS = arrayOf(
            EXTRA_SEQUENCE_TRIGGER_TIMEOUT,
            EXTRA_LONG_PRESS_DELAY,
            EXTRA_DOUBLE_PRESS_DELAY,
            EXTRA_VIBRATION_DURATION
        )

        val DESERIALIZER = jsonDeserializer {
            val triggerKeysJsonArray by it.json.byArray(NAME_KEYS)
            val keys = it.context.deserialize<List<Key>>(triggerKeysJsonArray)

            val extrasJsonArray by it.json.byArray(NAME_EXTRAS)
            val extraList = it.context.deserialize<List<Extra>>(extrasJsonArray) ?: listOf()

            val mode by it.json.byInt(NAME_MODE)

            val flags by it.json.byNullableInt(NAME_FLAGS)

            Trigger(keys, extraList, mode, flags ?: 0)
        }
    }

    class Key(
        @SerializedName(NAME_KEYCODE)
        val keyCode: Int,
        @SerializedName(NAME_DEVICE_ID)
        var deviceId: String = DEVICE_ID_THIS_DEVICE,

        @ClickType
        @SerializedName(NAME_CLICK_TYPE)
        var clickType: Int = SHORT_PRESS,

        @SerializedName(NAME_FLAGS)
        var flags: Int = 0
    ) {

        companion object {
            //DON'T CHANGE THESE. Used for JSON serialization and parsing.
            const val NAME_KEYCODE = "keyCode"
            const val NAME_DEVICE_ID = "deviceId"
            const val NAME_CLICK_TYPE = "clickType"
            const val NAME_FLAGS = "flags"

            //IDS! DON'T CHANGE
            const val DEVICE_ID_THIS_DEVICE = "io.github.sds100.keymapper.THIS_DEVICE"
            const val DEVICE_ID_ANY_DEVICE = "io.github.sds100.keymapper.ANY_DEVICE"

            const val FLAG_DO_NOT_CONSUME_KEY_EVENT = 1

            val TRIGGER_KEY_FLAG_LABEL_MAP = mapOf(
                FLAG_DO_NOT_CONSUME_KEY_EVENT to R.string.flag_dont_override_default_action
            )

            val DESERIALIZER = jsonDeserializer {
                val keycode by it.json.byInt(NAME_KEYCODE)
                val deviceId by it.json.byString(NAME_DEVICE_ID)
                val clickType by it.json.byInt(NAME_CLICK_TYPE)

                //nullable because this property was added after backup and restore was released.
                val flags by it.json.byNullableInt(NAME_FLAGS)

                Key(keycode, deviceId, clickType, flags ?: 0)
            }
        }

        val uniqueId: String
            get() = "$keyCode$clickType$deviceId"

        init {
            if (KeyEventUtils.isModifierKey(keyCode)) {
                flags = flags.withFlag(FLAG_DO_NOT_CONSUME_KEY_EVENT)
            }
        }

        override fun equals(other: Any?): Boolean {
            return (other as Key).keyCode == keyCode
        }

        override fun hashCode() = keyCode
    }

    fun clone(
        keys: List<Key> = this.keys,
        extras: List<Extra> = this.extras,
        @Mode mode: Int = this.mode,
        flags: Int = this.flags
    ) =
        Trigger(keys, extras, mode, flags)

    @IntDef(value = [PARALLEL, SEQUENCE, UNDEFINED])
    annotation class Mode

    @IntDef(value = [UNDETERMINED, SHORT_PRESS, LONG_PRESS, DOUBLE_PRESS])
    annotation class ClickType
}

fun sequenceTrigger(vararg key: Trigger.Key, flags: Int = 0) = Trigger(key.toList(), mode = Trigger.SEQUENCE, flags = flags)
fun undefinedTrigger(key: Trigger.Key, flags: Int = 0) = Trigger(listOf(key), mode = Trigger.UNDEFINED, flags = flags)
fun parallelTrigger(vararg key: Trigger.Key, flags: Int = 0) = Trigger(key.toList(), mode = Trigger.PARALLEL, flags = flags)