package io.github.sds100.keymapper.system.inputevents

import android.view.KeyEvent
import android.view.MotionEvent
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.devices.InputDeviceUtils

/**
 * This is our own abstraction over MotionEvent so that it is easier to write tests and read
 * values without relying on the Android SDK.
 */
data class MyMotionEvent(
    val eventTime: Long,
    val metaState: Int,
    val device: InputDeviceInfo,
    val axisHatX: Float,
    val axisHatY: Float,
    val isDpad: Boolean,
) {
    companion object {
        fun fromMotionEvent(event: MotionEvent): MyMotionEvent {
//            Timber.e("Hat x = ${event.getAxisValue(MotionEvent.AXIS_HAT_X)}, Hat y = ${event.getAxisValue(MotionEvent.AXIS_HAT_Y)}")
//            printMotionEventDetails(event)

            return MyMotionEvent(
                eventTime = event.eventTime,
                metaState = event.metaState,
                device = InputDeviceUtils.createInputDeviceInfo(event.device),
                axisHatX = event.getAxisValue(MotionEvent.AXIS_HAT_X),
                axisHatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y),
                isDpad = InputEventUtils.isDpadDevice(event),
            )
        }

        fun printMotionEventDetails(event: MotionEvent) {
            // Print the basic details of the MotionEvent
            println("Action: ${getActionName(event.action)}")
            println("Pointer Count: ${event.pointerCount}")
            println("Event Time: ${event.eventTime}")
            println("Down Time: ${event.downTime}")

            // Loop through all pointers (fingers) in the event
            for (i in 0 until event.pointerCount) {
                val pointerId = event.getPointerId(i)
                val x = event.getX(i)
                val y = event.getY(i)
                println("Pointer $pointerId: (x, y) = ($x, $y)")
            }

            // Print Axis values (useful for gamepads, joysticks, etc.)
            val axisNames = listOf(
                MotionEvent.AXIS_X, MotionEvent.AXIS_Y, MotionEvent.AXIS_Z,
                MotionEvent.AXIS_RX, MotionEvent.AXIS_RY, MotionEvent.AXIS_RZ,
                MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y,
                MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_RTRIGGER,
                MotionEvent.AXIS_THROTTLE, MotionEvent.AXIS_RUDDER,
                MotionEvent.AXIS_WHEEL, MotionEvent.AXIS_GAS,
                MotionEvent.AXIS_BRAKE,
            )

            for (axis in axisNames) {
                val axisValue = event.getAxisValue(axis)
                println("Axis $axis: $axisValue")
            }
        }

        // Helper function to convert action codes into human-readable names
        fun getActionName(actionCode: Int): String {
            return when (actionCode) {
                MotionEvent.ACTION_DOWN -> "ACTION_DOWN"
                MotionEvent.ACTION_UP -> "ACTION_UP"
                MotionEvent.ACTION_MOVE -> "ACTION_MOVE"
                MotionEvent.ACTION_CANCEL -> "ACTION_CANCEL"
                MotionEvent.ACTION_OUTSIDE -> "ACTION_OUTSIDE"
                MotionEvent.ACTION_POINTER_DOWN -> "ACTION_POINTER_DOWN"
                MotionEvent.ACTION_POINTER_UP -> "ACTION_POINTER_UP"
                else -> "UNKNOWN_ACTION"
            }
        }
    }

    // TODO move to DpadMotionEventTracker
    /**
     * Determine the corresponding DPAD key code from this motion event.
     *
     * See https://developer.android.com/develop/ui/views/touch-and-input/game-controllers/controller-input
     */
    fun getDpadKeyCode(): Int? {
        return when {
            // Check if the AXIS_HAT_X value is -1 or 1, and set the D-pad
            // LEFT and RIGHT direction accordingly.
            axisHatX.compareTo(-1.0f) == 0 -> KeyEvent.KEYCODE_DPAD_LEFT
            axisHatX.compareTo(1.0f) == 0 -> KeyEvent.KEYCODE_DPAD_RIGHT
            // Check if the AXIS_HAT_Y value is -1 or 1, and set the D-pad
            // UP and DOWN direction accordingly.
            axisHatY.compareTo(-1.0f) == 0 -> KeyEvent.KEYCODE_DPAD_UP
            axisHatY.compareTo(1.0f) == 0 -> KeyEvent.KEYCODE_DPAD_DOWN
            else -> null
        }
    }

//    fun convertToDpadKeyEvent(event: MotionEvent): MyKeyEvent? {
//        if (!isDpad) {
//            return null
//        }
//
//        val dpadKeyCode: Int? = when {
//            // Check if the AXIS_HAT_X value is -1 or 1, and set the D-pad
//            // LEFT and RIGHT direction accordingly.
//            axisHatX.compareTo(-1.0f) == 0 -> KeyEvent.KEYCODE_DPAD_LEFT
//            axisHatX.compareTo(1.0f) == 0 -> KeyEvent.KEYCODE_DPAD_RIGHT
//            // Check if the AXIS_HAT_Y value is -1 or 1, and set the D-pad
//            // UP and DOWN direction accordingly.
//            axisHatY.compareTo(-1.0f) == 0 -> KeyEvent.KEYCODE_DPAD_UP
//            axisHatY.compareTo(1.0f) == 0 -> KeyEvent.KEYCODE_DPAD_DOWN
//            else -> null
//        }
//
//        val keyEventAction = if (dpadKeyCode == null) {
//            KeyEvent.ACTION_UP
//        } else {
//            KeyEvent.ACTION_DOWN
//        }
//
//        return MyKeyEvent(
//            keyCode = dpadKeyCode,
//            action = keyEventAction,
//            metaState = metaState,
//            scanCode = 0,
//            device = device,
//            repeatCount = 0,
//        )
//    }
}
