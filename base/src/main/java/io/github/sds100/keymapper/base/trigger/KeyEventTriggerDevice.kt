package io.github.sds100.keymapper.base.trigger

import kotlinx.serialization.Serializable

@Serializable
sealed class KeyEventTriggerDevice : Comparable<KeyEventTriggerDevice> {
    override fun compareTo(other: KeyEventTriggerDevice) =
        this.javaClass.name.compareTo(other.javaClass.name)

    @Serializable
    data object Internal : KeyEventTriggerDevice()

    @Serializable
    data object Any : KeyEventTriggerDevice()

    @Serializable
    data class External(val descriptor: String, val name: String) : KeyEventTriggerDevice() {
        override fun compareTo(other: KeyEventTriggerDevice): Int {
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

    fun isSameDevice(other: KeyEventTriggerDevice): Boolean {
        if (this is Any || other is Any) {
            return true
        }

        if (this is External && other is External) {
            return this.descriptor == other.descriptor
        } else {
            return this is Internal && other is Internal
        }
    }
}
