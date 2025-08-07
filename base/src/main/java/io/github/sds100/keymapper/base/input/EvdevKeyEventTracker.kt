package io.github.sds100.keymapper.base.input

import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import io.github.sds100.keymapper.system.inputevents.KMEvdevEvent
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent

/**
 * This keeps track of which evdev events have been sent so evdev events
 * can be converted into key events with the correct metastate.
 */
class EvdevKeyEventTracker(
    private val inputDeviceCache: InputDeviceCache
) {

    fun toKeyEvent(event: KMEvdevEvent): KMKeyEvent? {
        if (!event.isKeyEvent) {
            return null
        }

        if (event.androidCode == null) {
            return null
        }

        val action = when (event.value) {
            0 -> KeyEvent.ACTION_UP
            1 -> KeyEvent.ACTION_DOWN
            2 -> KeyEvent.ACTION_MULTIPLE
            else -> throw IllegalArgumentException("Unknown evdev event value for keycode: ${event.value}")
        }

        val inputDevice = inputDeviceCache.getById(event.deviceId) ?: return null

        return KMKeyEvent(
            keyCode = event.androidCode!!,
            action = action,
            metaState = 0,     // TODO handle keeping track of metastate
            scanCode = event.code,
            device = inputDevice,
            repeatCount = 0, // TODO does this need handling?
            source = inputDevice.sources ?: InputDevice.SOURCE_UNKNOWN,// TODO
            eventTime = SystemClock.uptimeMillis()
        )
    }
}