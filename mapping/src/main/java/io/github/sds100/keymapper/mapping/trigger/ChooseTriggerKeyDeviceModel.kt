package io.github.sds100.keymapper.mapping.trigger

data class ChooseTriggerKeyDeviceModel(
    val triggerKeyUid: String,
    val devices: List<TriggerKeyDevice>,
)
