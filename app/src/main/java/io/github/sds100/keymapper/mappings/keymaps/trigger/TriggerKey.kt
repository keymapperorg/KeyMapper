package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.mappings.ClickType
import kotlinx.serialization.Serializable

@Serializable
sealed class TriggerKey : Comparable<TriggerKey> {
    abstract val clickType: ClickType

    /**
     * Whether the event that triggers this key will be consumed and not passed
     * onto subsequent apps. E.g consuming the volume down key event will mean the volume
     * doesn't change.
     */
    abstract val consumeEvent: Boolean
    abstract val uid: String

    fun setClickType(clickType: ClickType): TriggerKey = when (this) {
        is AssistantTriggerKey -> copy(clickType = clickType)
        is KeyCodeTriggerKey -> copy(clickType = clickType)
        is FloatingButtonKey -> copy(clickType = clickType)
        is FingerprintTriggerKey -> copy(clickType = clickType)
    }

    override fun compareTo(other: TriggerKey) = this.javaClass.name.compareTo(other.javaClass.name)
}
