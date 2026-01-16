package io.github.sds100.keymapper.system.inputevents

import android.view.KeyEvent
import io.github.sds100.keymapper.common.utils.withFlag

object KeyEventUtils {
    private val KEYCODES: IntArray by lazy { buildKeyCodeList() }

    private fun buildKeyCodeList(): IntArray {
        return IntArray(KeyEvent.getMaxKeyCode()) { it }
    }

    val MODIFIER_KEYCODES: Set<Int>
        get() = setOf(
            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_CTRL_LEFT,
            KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_META_LEFT,
            KeyEvent.KEYCODE_META_RIGHT,
            KeyEvent.KEYCODE_SYM,
            KeyEvent.KEYCODE_NUM,
            KeyEvent.KEYCODE_FUNCTION,
        )

    fun isModifierKey(keyCode: Int): Boolean = keyCode in MODIFIER_KEYCODES

    fun isGamepadKeyCode(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_C,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_Z,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_THUMBL,
            KeyEvent.KEYCODE_BUTTON_THUMBR,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_MODE,
            KeyEvent.KEYCODE_BUTTON_1,
            KeyEvent.KEYCODE_BUTTON_2,
            KeyEvent.KEYCODE_BUTTON_3,
            KeyEvent.KEYCODE_BUTTON_4,
            KeyEvent.KEYCODE_BUTTON_5,
            KeyEvent.KEYCODE_BUTTON_6,
            KeyEvent.KEYCODE_BUTTON_7,
            KeyEvent.KEYCODE_BUTTON_8,
            KeyEvent.KEYCODE_BUTTON_9,
            KeyEvent.KEYCODE_BUTTON_10,
            KeyEvent.KEYCODE_BUTTON_11,
            KeyEvent.KEYCODE_BUTTON_12,
            KeyEvent.KEYCODE_BUTTON_13,
            KeyEvent.KEYCODE_BUTTON_14,
            KeyEvent.KEYCODE_BUTTON_15,
            KeyEvent.KEYCODE_BUTTON_16,
                -> return true

            else -> return false
        }
    }

    /**
     * Get all the valid key codes which work on the Android version for the device.
     */
    fun getKeyCodes(): IntArray {
        return KEYCODES
    }

    fun modifierKeycodeToMetaState(modifier: Int) = when (modifier) {
        KeyEvent.KEYCODE_ALT_LEFT -> KeyEvent.META_ALT_LEFT_ON.withFlag(KeyEvent.META_ALT_ON)

        KeyEvent.KEYCODE_ALT_RIGHT -> KeyEvent.META_ALT_RIGHT_ON.withFlag(KeyEvent.META_ALT_ON)

        KeyEvent.KEYCODE_SHIFT_LEFT -> KeyEvent.META_SHIFT_LEFT_ON.withFlag(KeyEvent.META_SHIFT_ON)

        KeyEvent.KEYCODE_SHIFT_RIGHT -> KeyEvent.META_SHIFT_RIGHT_ON.withFlag(
            KeyEvent.META_SHIFT_ON,
        )

        KeyEvent.KEYCODE_SYM -> KeyEvent.META_SYM_ON

        KeyEvent.KEYCODE_FUNCTION -> KeyEvent.META_FUNCTION_ON

        KeyEvent.KEYCODE_CTRL_LEFT -> KeyEvent.META_CTRL_LEFT_ON.withFlag(KeyEvent.META_CTRL_ON)

        KeyEvent.KEYCODE_CTRL_RIGHT -> KeyEvent.META_CTRL_RIGHT_ON.withFlag(KeyEvent.META_CTRL_ON)

        KeyEvent.KEYCODE_META_LEFT -> KeyEvent.META_META_LEFT_ON.withFlag(KeyEvent.META_META_ON)

        KeyEvent.KEYCODE_META_RIGHT -> KeyEvent.META_META_RIGHT_ON.withFlag(KeyEvent.META_META_ON)

        KeyEvent.KEYCODE_CAPS_LOCK -> KeyEvent.META_CAPS_LOCK_ON

        KeyEvent.KEYCODE_NUM_LOCK -> KeyEvent.META_NUM_LOCK_ON

        KeyEvent.KEYCODE_SCROLL_LOCK -> KeyEvent.META_SCROLL_LOCK_ON

        else -> throw Exception("can't convert modifier $modifier to meta state")
    }

    fun isDpadKeyCode(code: Int): Boolean {
        return code == KeyEvent.KEYCODE_DPAD_LEFT ||
            code == KeyEvent.KEYCODE_DPAD_RIGHT ||
            code == KeyEvent.KEYCODE_DPAD_UP ||
            code == KeyEvent.KEYCODE_DPAD_DOWN ||
            code == KeyEvent.KEYCODE_DPAD_UP_LEFT ||
            code == KeyEvent.KEYCODE_DPAD_UP_RIGHT ||
            code == KeyEvent.KEYCODE_DPAD_DOWN_LEFT ||
            code == KeyEvent.KEYCODE_DPAD_DOWN_RIGHT ||
            code == KeyEvent.KEYCODE_DPAD_CENTER
    }

    fun isGamepadButton(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_C,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_Z,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_THUMBL,
            KeyEvent.KEYCODE_BUTTON_THUMBR,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_MODE,
            KeyEvent.KEYCODE_BUTTON_1,
            KeyEvent.KEYCODE_BUTTON_2,
            KeyEvent.KEYCODE_BUTTON_3,
            KeyEvent.KEYCODE_BUTTON_4,
            KeyEvent.KEYCODE_BUTTON_5,
            KeyEvent.KEYCODE_BUTTON_6,
            KeyEvent.KEYCODE_BUTTON_7,
            KeyEvent.KEYCODE_BUTTON_8,
            KeyEvent.KEYCODE_BUTTON_9,
            KeyEvent.KEYCODE_BUTTON_10,
            KeyEvent.KEYCODE_BUTTON_11,
            KeyEvent.KEYCODE_BUTTON_12,
            KeyEvent.KEYCODE_BUTTON_13,
            KeyEvent.KEYCODE_BUTTON_14,
            KeyEvent.KEYCODE_BUTTON_15,
            KeyEvent.KEYCODE_BUTTON_16,
                -> true

            else -> false
        }
    }

    fun isKeyCodeUnknown(keyCode: Int): Boolean {
        // The lowest key code is 1 (KEYCODE_SOFT_LEFT)
        return keyCode > KeyEvent.getMaxKeyCode() || keyCode < 1
    }

    fun isPowerButtonKey(keyCode: Int, scanCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_POWER ||
            keyCode == KeyEvent.KEYCODE_TV_POWER ||
            scanCode == Scancode.KEY_POWER ||
            scanCode == Scancode.KEY_POWER2
    }
}
