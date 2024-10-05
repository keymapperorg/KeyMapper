package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.data.entities.AssistantTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity
import io.github.sds100.keymapper.mappings.ClickType
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AssistantTriggerKey(
    override val uid: String = UUID.randomUUID().toString(),
    val type: AssistantTriggerType,
    override val clickType: ClickType,

    val consumeKeyEvent: Boolean = true,
) : TriggerKey() {

    companion object {
        fun fromEntity(
            entity: AssistantTriggerKeyEntity,
        ): TriggerKey {
            val type: AssistantTriggerType = when (entity.type) {
                AssistantTriggerKeyEntity.ASSISTANT_TYPE_VOICE -> AssistantTriggerType.VOICE
                AssistantTriggerKeyEntity.ASSISTANT_TYPE_DEVICE -> AssistantTriggerType.DEVICE
                else -> AssistantTriggerType.ANY
            }

            val clickType: ClickType = when (entity.clickType) {
                TriggerKeyEntity.SHORT_PRESS -> ClickType.SHORT_PRESS
                TriggerKeyEntity.LONG_PRESS -> ClickType.LONG_PRESS
                TriggerKeyEntity.DOUBLE_PRESS -> ClickType.DOUBLE_PRESS
                else -> ClickType.SHORT_PRESS
            }

            return AssistantTriggerKey(
                uid = entity.uid,
                type = type,
                clickType = clickType,
            )
        }

        fun toEntity(key: AssistantTriggerKey): AssistantTriggerKeyEntity {
            val type: String = when (key.type) {
                AssistantTriggerType.VOICE -> AssistantTriggerKeyEntity.ASSISTANT_TYPE_VOICE
                AssistantTriggerType.DEVICE -> AssistantTriggerKeyEntity.ASSISTANT_TYPE_DEVICE
                AssistantTriggerType.ANY -> AssistantTriggerKeyEntity.ASSISTANT_TYPE_ANY
            }

            val clickType: Int = when (key.clickType) {
                ClickType.SHORT_PRESS -> TriggerKeyEntity.SHORT_PRESS
                ClickType.LONG_PRESS -> TriggerKeyEntity.LONG_PRESS
                ClickType.DOUBLE_PRESS -> TriggerKeyEntity.DOUBLE_PRESS
            }

            return AssistantTriggerKeyEntity(
                type = type,
                clickType = clickType,
                uid = key.uid,
            )
        }
    }
}
