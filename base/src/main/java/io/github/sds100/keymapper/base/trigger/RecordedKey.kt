package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.input.InputEventDetectionSource
import io.github.sds100.keymapper.common.models.EvdevDeviceHandle

sealed class RecordedKey {
    data class KeyEvent(
        val keyCode: Int,
        val scanCode: Int,
        val deviceDescriptor: String,
        val deviceName: String,
        val isExternalDevice: Boolean,
        val detectionSource: InputEventDetectionSource,
    ) : RecordedKey()

    data class EvdevEvent(val keyCode: Int, val scanCode: Int, val device: EvdevDeviceHandle) :
        RecordedKey()
}
