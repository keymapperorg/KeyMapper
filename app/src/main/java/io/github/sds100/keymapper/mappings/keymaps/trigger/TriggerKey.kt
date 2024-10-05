package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.data.entities.AssistantTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.KeyCodeTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity
import io.github.sds100.keymapper.mappings.ClickType

sealed class TriggerKey {
    abstract val clickType: ClickType
    abstract val uid: String

    companion object {
        fun fromEntity(entity: TriggerKeyEntity): TriggerKey {
            return when (entity) {
                is AssistantTriggerKeyEntity -> AssistantTriggerKey.fromEntity(entity)
                is KeyCodeTriggerKeyEntity -> KeyCodeTriggerKey.fromEntity(entity)
            }
        }

        fun toEntity(key: TriggerKey): TriggerKeyEntity {
            return when (key) {
                is AssistantTriggerKey -> AssistantTriggerKey.toEntity(key)
                is KeyCodeTriggerKey -> KeyCodeTriggerKey.toEntity(key)
            }
        }
    }
}
