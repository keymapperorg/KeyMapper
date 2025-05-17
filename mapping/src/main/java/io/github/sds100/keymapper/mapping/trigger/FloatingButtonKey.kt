package io.github.sds100.keymapper.mapping.trigger

import io.github.sds100.keymapper.data.entities.FloatingButtonEntityWithLayout
import io.github.sds100.keymapper.data.entities.FloatingButtonKeyEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity
import io.github.sds100.keymapper.floating.FloatingButtonData
import io.github.sds100.keymapper.floating.FloatingButtonEntityMapper
import io.github.sds100.keymapper.base.keymaps.ClickType
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class FloatingButtonKey(
    override val uid: String = UUID.randomUUID().toString(),
    val buttonUid: String,
    val button: FloatingButtonData?,
    override val clickType: ClickType,
) : TriggerKey() {

    override val consumeEvent: Boolean = true
    override val allowedLongPress: Boolean = true
    override val allowedDoublePress: Boolean = true

    override fun compareTo(other: TriggerKey) = when (other) {
        is FloatingButtonKey -> compareValuesBy(
            this,
            other,
            { it.uid },
            { it.clickType },
        )

        else -> super.compareTo(other)
    }

    companion object {
        fun fromEntity(
            entity: FloatingButtonKeyEntity,
            buttonEntity: FloatingButtonEntityWithLayout?,
        ): TriggerKey {
            val clickType = when (entity.clickType) {
                TriggerKeyEntity.SHORT_PRESS -> ClickType.SHORT_PRESS
                TriggerKeyEntity.LONG_PRESS -> ClickType.LONG_PRESS
                TriggerKeyEntity.DOUBLE_PRESS -> ClickType.DOUBLE_PRESS
                else -> ClickType.SHORT_PRESS
            }
            return FloatingButtonKey(
                uid = entity.uid,
                buttonUid = entity.buttonUid,
                button = buttonEntity?.let { buttonEntity ->
                    FloatingButtonEntityMapper.fromEntity(
                        buttonEntity.button,
                        buttonEntity.layout.name,
                    )
                },
                clickType = clickType,
            )
        }

        fun toEntity(key: FloatingButtonKey): FloatingButtonKeyEntity {
            val clickType = when (key.clickType) {
                ClickType.SHORT_PRESS -> TriggerKeyEntity.SHORT_PRESS
                ClickType.LONG_PRESS -> TriggerKeyEntity.LONG_PRESS
                ClickType.DOUBLE_PRESS -> TriggerKeyEntity.DOUBLE_PRESS
            }

            return FloatingButtonKeyEntity(
                uid = key.uid,
                buttonUid = key.buttonUid,
                clickType = clickType,
            )
        }
    }
}
