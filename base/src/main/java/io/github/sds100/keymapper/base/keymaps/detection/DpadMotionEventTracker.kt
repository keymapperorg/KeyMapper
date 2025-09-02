package io.github.sds100.keymapper.base.keymaps.detection

import android.view.InputDevice
import android.view.KeyEvent
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.system.inputevents.MyKeyEvent
import io.github.sds100.keymapper.system.inputevents.MyMotionEvent

/**
 * See https://developer.android.com/develop/ui/views/touch-and-input/game-controllers/controller-input#dpad
 * Some controllers send motion events as well as key events when DPAD buttons
 * are pressed, while others just send key events.
 * The motion events must be consumed but this means the following key events are also
 * consumed so one must rely on converting these motion events oneself.
 */
class DpadMotionEventTracker {
    companion object {
        private const val DPAD_DOWN = 1
        private const val DPAD_UP = 2
        private const val DPAD_LEFT = 4
        private const val DPAD_RIGHT = 8
    }

    val dpadState: HashMap<String, Int> = hashMapOf()

    /**
     * When moving the joysticks on a controller while pressing a DPAD button at the same time,
     * DPAD key events can be interleaved in the DPAD motion events. We don't want to register
     * these as multiple clicks so consume DPAD key events if the motion events say the DPAD
     * is already pressed.
     *
     * @return whether to consume the key event.
     */
    fun onKeyEvent(event: MyKeyEvent): Boolean {
        val device = event.device ?: return false

        if (!InputEventUtils.isDpadKeyCode(event.keyCode)) {
            return false
        }

        val dpadFlag = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> DPAD_DOWN
            KeyEvent.KEYCODE_DPAD_UP -> DPAD_UP
            KeyEvent.KEYCODE_DPAD_LEFT -> DPAD_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> DPAD_RIGHT
            else -> return false
        }

        val dpadState = dpadState[device.descriptor] ?: return false

        if (dpadState == 0) {
            return false
        }

        return dpadState and dpadFlag == dpadFlag
    }

    /**
     * The equivalent DPAD key events if any DPAD buttons changed in the motion event.
     * There is a chance that one motion event will be sent if multiple axes change at the same time
     * hence why it returns an array.
     *
     * @return An array of key events. Empty if no DPAD buttons changed.
     */
    fun convertMotionEvent(event: MyMotionEvent): List<MyKeyEvent> {
        val oldState = dpadState[event.device.getDescriptor()] ?: 0
        val newState = eventToDpadState(event)
        val diff = oldState xor newState

        dpadState[event.device.getDescriptor()] = newState

        // If no dpad keys changed then return null
        if (diff == 0) {
            return emptyList()
        }

        val keyCodes = mutableListOf<Int>()

        if (diff and DPAD_DOWN == DPAD_DOWN) {
            keyCodes.add(KeyEvent.KEYCODE_DPAD_DOWN)
        }

        if (diff and DPAD_UP == DPAD_UP) {
            keyCodes.add(KeyEvent.KEYCODE_DPAD_UP)
        }

        if (diff and DPAD_LEFT == DPAD_LEFT) {
            keyCodes.add(KeyEvent.KEYCODE_DPAD_LEFT)
        }

        if (diff and DPAD_RIGHT == DPAD_RIGHT) {
            keyCodes.add(KeyEvent.KEYCODE_DPAD_RIGHT)
        }

        // If the new state contains the dpad press then it has just been pressed down.
        val action = if (newState and diff == diff) {
            KeyEvent.ACTION_DOWN
        } else {
            KeyEvent.ACTION_UP
        }

        return keyCodes.map {
            MyKeyEvent(
                it,
                action,
                metaState = event.metaState,
                scanCode = 0,
                device = event.device,
                repeatCount = 0,
                source = InputDevice.SOURCE_DPAD,
            )
        }
    }

    fun reset() {
        dpadState.clear()
    }

    private fun InputDeviceInfo?.getDescriptor(): String {
        return this?.descriptor ?: ""
    }

    private fun eventToDpadState(event: MyMotionEvent): Int {
        var state = 0

        if (event.axisHatX == -1.0f) {
            state = DPAD_LEFT
        } else if (event.axisHatX == 1.0f) {
            state = DPAD_RIGHT
        }

        if (event.axisHatY == -1.0f) {
            state = state or DPAD_UP
        } else if (event.axisHatY == 1.0f) {
            state = state or DPAD_DOWN
        }

        return state
    }
}
