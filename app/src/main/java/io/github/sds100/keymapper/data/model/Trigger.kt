package io.github.sds100.keymapper.data.model

import androidx.annotation.IntDef
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * @property [keys] The key codes which will trigger the action
 */
class Trigger(keys: List<Key> = listOf()) {

    companion object {
        const val PARALLEL = 0
        const val SEQUENCE = 1

        const val DEFAULT_TRIGGER_MODE = PARALLEL

        const val SHORT_PRESS = 0
        const val LONG_PRESS = 1
        const val DOUBLE_PRESS = 2

        val CLICK_TYPE_LABEL_MAP = mapOf(
            SHORT_PRESS to R.string.clicktype_short_press,
            LONG_PRESS to R.string.clicktype_long_press,
            DOUBLE_PRESS to R.string.clicktype_double_press
        )
    }

    var keys: List<Key> = keys
        set(value) {
            //trigger keys can only be short pressed if the trigger is in parallel mode
            if (mode == PARALLEL) {
                value.forEach { it.clickType = SHORT_PRESS }
            }

            field = value
        }

    @Mode
    var mode: Int = DEFAULT_TRIGGER_MODE
        set(value) {
            //trigger keys can only be short pressed if the trigger is in parallel mode
            if (value == PARALLEL) {
                keys.forEach { it.clickType = SHORT_PRESS }
            }

            field = value
        }

    data class Key(val keyCode: Int, var deviceId: String? = null, @ClickType var clickType: Int = SHORT_PRESS) {
        override fun equals(other: Any?): Boolean {
            return (other as Key).keyCode == keyCode
        }

        override fun hashCode() = keyCode
    }

    @IntDef(value = [PARALLEL, SEQUENCE])
    annotation class Mode

    @IntDef(value = [SHORT_PRESS, LONG_PRESS, DOUBLE_PRESS])
    annotation class ClickType
}