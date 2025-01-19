package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import kotlinx.serialization.Serializable

@Serializable
sealed class TriggerKeyDevice {
    @Serializable
    object Internal : TriggerKeyDevice()

    @Serializable
    object Any : TriggerKeyDevice()

    @Serializable
    data class External(val descriptor: String, val name: String) : TriggerKeyDevice()

    fun isSameDevice(other: TriggerKeyDevice): Boolean {
        if (other is External && this is External) {
            return other.descriptor == this.descriptor
        } else {
            return true
        }
    }

    fun isSameDevice(device: InputDeviceInfo): Boolean {
        if (this is External && device.isExternal) {
            return device.descriptor == this.descriptor
        } else {
            return true
        }
    }
}
