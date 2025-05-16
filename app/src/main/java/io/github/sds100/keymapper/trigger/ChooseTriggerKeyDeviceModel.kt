package io.github.sds100.keymapper.trigger

data class ChooseTriggerKeyDeviceModel(
    val triggerKeyUid: String,
    val devices: List<TriggerKeyDevice>,
)
