package io.github.sds100.keymapper.trigger

/**
 * Created by sds100 on 07/03/2021.
 */
data class ChooseTriggerKeyDeviceModel(
    val triggerKeyUid: String,
    val devices: List<TriggerKeyDevice>,
)
