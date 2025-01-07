package io.github.sds100.keymapper.mappings.keymaps.detection

import android.view.KeyEvent
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

    private val dpadState: HashMap<String, Int> = hashMapOf()

    /**
     * @return The equivalent DPAD key event if there was one in response to this motion event.
     * Null if this motion event didn't result in a DPAD key event.
     */
    fun convertMotionEvent(event: MyMotionEvent): MyKeyEvent? {
        val oldState = dpadState[event.device.descriptor] ?: 0
        val newState = eventToDpadState(event)
        val diff = oldState xor newState

        dpadState[event.device.descriptor] = newState

        // If no dpad keys changed then return null
        if (diff == 0) {
            return null
        }

        val keyCode = when (diff) {
            DPAD_DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
            DPAD_UP -> KeyEvent.KEYCODE_DPAD_UP
            DPAD_LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
            DPAD_RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
            else -> throw Exception("Can't convert this dpad flag diff $diff")
        }

        // If the new state contains the dpad press then it has just been pressed down.
        val action = if (newState and diff == diff) {
            KeyEvent.ACTION_DOWN
        } else {
            KeyEvent.ACTION_UP
        }

        return MyKeyEvent(
            keyCode,
            action,
            metaState = event.metaState,
            scanCode = 0,
            device = event.device,
            repeatCount = 0,
        )
    }

    fun reset() {
        dpadState.clear()
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
