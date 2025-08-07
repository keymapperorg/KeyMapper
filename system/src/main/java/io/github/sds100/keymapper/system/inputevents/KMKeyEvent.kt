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
    val device: InputDeviceInfo,
    val repeatCount: Int,
    val source: Int,
    val eventTime: Long,
) : KMInputEvent {

    companion object {
        fun fromKeyEvent(keyEvent: KeyEvent): KMKeyEvent? {
            val device = keyEvent.device ?: return null

            return KMKeyEvent(
                keyCode = keyEvent.keyCode,
                action = keyEvent.action,
                metaState = keyEvent.metaState,
                scanCode = keyEvent.scanCode,
                device = InputDeviceUtils.createInputDeviceInfo(device),
                repeatCount = keyEvent.repeatCount,
                source = keyEvent.source,
                eventTime = keyEvent.eventTime,
            )
        }
    }

    override val deviceId: Int = device.id

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
