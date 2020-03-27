package io.github.sds100.keymapper.data.model

/**
 * Created by sds100 on 27/03/2020.
 */
data class TriggerKeyModel(
    val name: String,
    @Trigger.ClickType val clickType: Int,
    val deviceName: String? = null
)