package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import kotlinx.serialization.Serializable



@Serializable
sealed class TriggerKeyDevice : Comparable<TriggerKeyDevice> {
    override fun compareTo(other: TriggerKeyDevice) = this.javaClass.name.compareTo(other.javaClass.name)

    @Serializable
    data object Internal : TriggerKeyDevice()

    @Serializable
    data object Any : TriggerKeyDevice()

    @Serializable
    data class External(val descriptor: String, val name: String) : TriggerKeyDevice() {
        override fun compareTo(other: TriggerKeyDevice): Int {
            if (other !is External) {
                return super.compareTo(other)
            }

            return compareValuesBy(
                this,
                other,
                { it.name },
                { it.descriptor },
            )
        }
    }

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
