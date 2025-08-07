package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.input.InputEventDetectionSource

data class RecordedKey(
    val keyCode: Int,
    val scanCode: Int,
    val deviceDescriptor: String,
    val deviceName: String,
    val isExternalDevice: Boolean,
    val detectionSource: InputEventDetectionSource,
)
