package io.github.sds100.keymapper.trigger


data class RecordedKey(
    val keyCode: Int,
    val device: TriggerKeyDevice,
    val detectionSource: KeyEventDetectionSource,
)
