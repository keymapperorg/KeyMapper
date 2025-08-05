package io.github.sds100.keymapper.system.inputevents

import android.view.MotionEvent
import io.github.sds100.keymapper.common.utils.InputDeviceInfo
import io.github.sds100.keymapper.common.utils.InputDeviceUtils

/**
 * This is our own abstraction over MotionEvent so that it is easier to write tests and read
 * values without relying on the Android SDK.
 */
data class KMGamePadEvent(
    val eventTime: Long,
    val metaState: Int,
    val device: InputDeviceInfo?,
    val axisHatX: Float,
    val axisHatY: Float,
) : KMInputEvent {

    constructor(event: MotionEvent) : this(
        eventTime = event.eventTime,
        metaState = event.metaState,
        device = event.device?.let { InputDeviceUtils.createInputDeviceInfo(it) },
        axisHatX = event.getAxisValue(MotionEvent.AXIS_HAT_X),
        axisHatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y),
    )
}
