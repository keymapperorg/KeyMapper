package io.github.sds100.keymapper.system.inputevents

import android.view.KeyEvent
import io.github.sds100.keymapper.common.utils.InputDeviceInfo
import io.github.sds100.keymapper.common.utils.InputDeviceUtils

/**
 * This is our own abstraction over KeyEvent so that it is easier to write tests and read
 * values without relying on the Android SDK.
 */
data class KMKeyEvent(
    val keyCode: Int,
    val action: Int,
    val metaState: Int,
    val scanCode: Int,
    val device: InputDeviceInfo?,
    val repeatCount: Int,
    val source: Int,
    val eventTime: Long
) : KMInputEvent {

    constructor(keyEvent: KeyEvent) : this(
        keyCode = keyEvent.keyCode,
        action = keyEvent.action,
        metaState = keyEvent.metaState,
        scanCode = keyEvent.scanCode,
        device = keyEvent.device?.let { InputDeviceUtils.createInputDeviceInfo(it) },
        repeatCount = keyEvent.repeatCount,
        source = keyEvent.source,
        eventTime = keyEvent.eventTime
    )

    fun toKeyEvent(): KeyEvent {
        return KeyEvent(
            eventTime,
            eventTime,
            action,
            keyCode,
            repeatCount,
            metaState,
            device?.id ?: -1,
            scanCode,
            source
        )
    }
}
