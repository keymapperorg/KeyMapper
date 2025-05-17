package io.github.sds100.keymapper.base.trigger

data class ChooseTriggerKeyDeviceModel(
    val triggerKeyUid: String,
    val devices: List<TriggerKeyDevice>,
)
