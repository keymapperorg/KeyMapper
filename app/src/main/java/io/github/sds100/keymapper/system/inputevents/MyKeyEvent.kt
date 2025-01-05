package io.github.sds100.keymapper.system.inputevents

import io.github.sds100.keymapper.system.devices.InputDeviceInfo

data class MyKeyEvent(
    val keyCode: Int,
    val action: Int,
    val metaState: Int,
    val scanCode: Int,
    val device: InputDeviceInfo,
    val repeatCount: Int,
)
