package io.github.sds100.keymapper.mapping.trigger

import io.github.sds100.keymapper.data.entities.FingerprintTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.keymaps.FingerprintGestureType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class FingerprintTriggerKey(
    override val uid: String = UUID.randomUUID().toString(),

    @SerialName("fingerprintGestureType")
    val type: FingerprintGestureType,

    override val clickType: ClickType,
) : TriggerKey() {
    override val consumeEvent: Boolean = true
    override val allowedLongPress: Boolean = false
    override val allowedDoublePress: Boolean = false

    override fun compareTo(other: TriggerKey) = when (other) {
        is FingerprintTriggerKey -> compareValuesBy(
            this,
            other,
            { it.type },
            { it.clickType },
        )

        else -> super.compareTo(other)
    }

    companion object {
        fun fromEntity(
            entity: FingerprintTriggerKeyEntity,
        ): TriggerKey {
            val type: FingerprintGestureType = when (entity.type) {
                FingerprintTriggerKeyEntity.ID_SWIPE_DOWN -> FingerprintGestureType.SWIPE_DOWN
                FingerprintTriggerKeyEntity.ID_SWIPE_UP -> FingerprintGestureType.SWIPE_UP
                FingerprintTriggerKeyEntity.ID_SWIPE_LEFT -> FingerprintGestureType.SWIPE_LEFT
                FingerprintTriggerKeyEntity.ID_SWIPE_RIGHT -> FingerprintGestureType.SWIPE_RIGHT
                else -> FingerprintGestureType.SWIPE_DOWN
            }

            val clickType: ClickType = when (entity.clickType) {
                TriggerKeyEntity.SHORT_PRESS -> ClickType.SHORT_PRESS
                TriggerKeyEntity.LONG_PRESS -> ClickType.LONG_PRESS
                TriggerKeyEntity.DOUBLE_PRESS -> ClickType.DOUBLE_PRESS
                else -> ClickType.SHORT_PRESS
            }

            return FingerprintTriggerKey(
                uid = entity.uid,
                type = type,
                clickType = clickType,
            )
        }

        fun toEntity(key: FingerprintTriggerKey): FingerprintTriggerKeyEntity {
            val type: Int = when (key.type) {
                FingerprintGestureType.SWIPE_DOWN -> FingerprintTriggerKeyEntity.ID_SWIPE_DOWN
                FingerprintGestureType.SWIPE_UP -> FingerprintTriggerKeyEntity.ID_SWIPE_UP
                FingerprintGestureType.SWIPE_LEFT -> FingerprintTriggerKeyEntity.ID_SWIPE_LEFT
                FingerprintGestureType.SWIPE_RIGHT -> FingerprintTriggerKeyEntity.ID_SWIPE_RIGHT
            }

            val clickType: Int = when (key.clickType) {
                ClickType.SHORT_PRESS -> TriggerKeyEntity.SHORT_PRESS
                ClickType.LONG_PRESS -> TriggerKeyEntity.LONG_PRESS
                ClickType.DOUBLE_PRESS -> TriggerKeyEntity.DOUBLE_PRESS
            }

            return FingerprintTriggerKeyEntity(
                type = type,
                clickType = clickType,
                uid = key.uid,
            )
        }
    }
}
