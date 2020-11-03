package io.github.sds100.keymapper.data.model

/**
 * Created by sds100 on 27/03/2020.
 */
data class TriggerKeyModel(
    val id: String,
    val keyCode: Int,
    val name: String,
    @Trigger.ClickType val clickType: Int,
    val extraInfo: String
)