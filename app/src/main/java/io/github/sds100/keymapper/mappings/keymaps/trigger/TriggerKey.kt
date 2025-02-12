package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.data.entities.AssistantTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.KeyCodeTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity
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

    companion object {
        fun fromEntity(entity: TriggerKeyEntity): TriggerKey = when (entity) {
            is AssistantTriggerKeyEntity -> AssistantTriggerKey.fromEntity(entity)
            is KeyCodeTriggerKeyEntity -> KeyCodeTriggerKey.fromEntity(entity)
        }

        fun toEntity(key: TriggerKey): TriggerKeyEntity = when (key) {
            is AssistantTriggerKey -> AssistantTriggerKey.toEntity(key)
            is KeyCodeTriggerKey -> KeyCodeTriggerKey.toEntity(key)
        }
    }

    fun setClickType(clickType: ClickType): TriggerKey = when (this) {
        is AssistantTriggerKey -> copy(clickType = clickType)
        is KeyCodeTriggerKey -> copy(clickType = clickType)
    }

    override fun compareTo(other: TriggerKey) = this.javaClass.name.compareTo(other.javaClass.name)
}
