package io.github.sds100.keymapper.mappings.keymaps.trigger

import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 21/02/2021.
 */

@Serializable
sealed class TriggerKeyDevice {
    @Serializable
    object Internal : TriggerKeyDevice()

    @Serializable
    object Any : TriggerKeyDevice()

    @Serializable
    data class External(val descriptor: String, val name: String) : TriggerKeyDevice()
}
