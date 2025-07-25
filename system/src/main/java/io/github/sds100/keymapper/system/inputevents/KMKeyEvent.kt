package io.github.sds100.keymapper.system.inputevents

import io.github.sds100.keymapper.system.devices.InputDeviceInfo

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
) : KMInputEvent
