package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.input.InputEventDetectionSource

data class RecordedKey(
    val keyCode: Int,
    val device: TriggerKeyDevice,
    val detectionSource: InputEventDetectionSource,
)
