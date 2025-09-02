package io.github.sds100.keymapper.system.inputevents

import android.view.MotionEvent
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.devices.InputDeviceUtils

/**
 * This is our own abstraction over MotionEvent so that it is easier to write tests and read
 * values without relying on the Android SDK.
 */
data class MyMotionEvent(
    val metaState: Int,
    val device: InputDeviceInfo?,
    val axisHatX: Float,
    val axisHatY: Float,
    val isDpad: Boolean,
) {
    companion object {
        fun fromMotionEvent(event: MotionEvent): MyMotionEvent {
            return MyMotionEvent(
                metaState = event.metaState,
                device = event.device?.let { InputDeviceUtils.createInputDeviceInfo(it) },
                axisHatX = event.getAxisValue(MotionEvent.AXIS_HAT_X),
                axisHatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y),
                isDpad = InputEventUtils.isDpadDevice(event),
            )
        }
    }
}
