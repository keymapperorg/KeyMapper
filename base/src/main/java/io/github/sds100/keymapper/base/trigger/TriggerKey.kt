package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.keymaps.ClickType
import kotlinx.serialization.Serializable

@Serializable
sealed class TriggerKey : Comparable<TriggerKey> {
    abstract val clickType: ClickType

    abstract val uid: String
    abstract val allowedLongPress: Boolean
    abstract val allowedDoublePress: Boolean

    fun setClickType(clickType: ClickType): TriggerKey = when (this) {
        is AssistantTriggerKey -> copy(clickType = clickType)
        is KeyEventTriggerKey -> copy(clickType = clickType)
        is FloatingButtonKey -> copy(clickType = clickType)
        is FingerprintTriggerKey -> copy(clickType = clickType)
        is EvdevTriggerKey -> copy(clickType = clickType)
    }

    override fun compareTo(other: TriggerKey) = this.javaClass.name.compareTo(other.javaClass.name)

    fun isLogicallyEqual(other: TriggerKey): Boolean {
        when {
            this is KeyCodeTriggerKey && other is KeyCodeTriggerKey -> {
                if (this.detectWithScancode()) {
                    return this.scanCode == other.scanCode && this.isSameDevice(other)
                } else {
                    return this.keyCode == other.keyCode && this.isSameDevice(other)
                }
            }

            // Assistant triggers can not be triggered at the same time.
            this is AssistantTriggerKey && other is AssistantTriggerKey -> {
                return true
            }

            // Fingerprint gestures can not be triggered at the same time.
            this is FingerprintTriggerKey && other is FingerprintTriggerKey -> {
                return true
            }

            this is FloatingButtonKey && other is FloatingButtonKey -> {
                return this.buttonUid == other.buttonUid
            }
        }

        return false
    }
}
