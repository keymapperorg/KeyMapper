package io.github.sds100.keymapper.mappings.keymaps.trigger

/**
 * Created by sds100 on 04/03/2021.
 */
data class RecordedKey(
    val keyCode: Int,
    val device: TriggerKeyDevice,
    val detectionSource: KeyEventDetectionSource,
)
