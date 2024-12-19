package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.data.entities.KeyCodeTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity
import io.github.sds100.keymapper.mappings.ClickType
import kotlinx.serialization.Serializable
import splitties.bitflags.hasFlag
import splitties.bitflags.withFlag
import java.util.UUID

@Serializable
data class KeyCodeTriggerKey(
    override val uid: String = UUID.randomUUID().toString(),
    val keyCode: Int,
    val device: TriggerKeyDevice,
    override val clickType: ClickType,
    override val consumeEvent: Boolean = true,
) : TriggerKey() {

    override fun toString(): String {
        val deviceString = when (device) {
            TriggerKeyDevice.Any -> "any"
            is TriggerKeyDevice.External -> "external"
            TriggerKeyDevice.Internal -> "internal"
        }
        return "KeyCodeTriggerKey(uid=${uid.substring(0..5)}, keyCode=$keyCode, device=$deviceString, clickType=$clickType, consume=$consumeEvent) "
    }

    // key code -> click type -> device -> consume key event
    override fun compareTo(other: TriggerKey) = when (other) {
        is KeyCodeTriggerKey -> compareValuesBy(
            this,
            other,
            { it.keyCode },
            { it.clickType },
            { it.device },
            { it.consumeEvent },
        )

        else -> super.compareTo(other)
    }

    companion object {
        fun fromEntity(entity: KeyCodeTriggerKeyEntity): TriggerKey = KeyCodeTriggerKey(
            uid = entity.uid,
            keyCode = entity.keyCode,
            device = when (entity.deviceId) {
                KeyCodeTriggerKeyEntity.DEVICE_ID_THIS_DEVICE -> TriggerKeyDevice.Internal
                KeyCodeTriggerKeyEntity.DEVICE_ID_ANY_DEVICE -> TriggerKeyDevice.Any
                else -> TriggerKeyDevice.External(
                    entity.deviceId,
                    entity.deviceName ?: "",
                )
            },
            clickType = when (entity.clickType) {
                TriggerKeyEntity.SHORT_PRESS -> ClickType.SHORT_PRESS
                TriggerKeyEntity.LONG_PRESS -> ClickType.LONG_PRESS
                TriggerKeyEntity.DOUBLE_PRESS -> ClickType.DOUBLE_PRESS
                else -> ClickType.SHORT_PRESS
            },
            consumeEvent = !entity.flags.hasFlag(KeyCodeTriggerKeyEntity.FLAG_DO_NOT_CONSUME_KEY_EVENT),
        )

        fun toEntity(key: KeyCodeTriggerKey): KeyCodeTriggerKeyEntity {
            val deviceId = when (key.device) {
                TriggerKeyDevice.Any -> KeyCodeTriggerKeyEntity.DEVICE_ID_ANY_DEVICE
                is TriggerKeyDevice.External -> key.device.descriptor
                TriggerKeyDevice.Internal -> KeyCodeTriggerKeyEntity.DEVICE_ID_THIS_DEVICE
            }

            val deviceName = if (key.device is TriggerKeyDevice.External) {
                key.device.name
            } else {
                null
            }

            val clickType = when (key.clickType) {
                ClickType.SHORT_PRESS -> TriggerKeyEntity.SHORT_PRESS
                ClickType.LONG_PRESS -> TriggerKeyEntity.LONG_PRESS
                ClickType.DOUBLE_PRESS -> TriggerKeyEntity.DOUBLE_PRESS
            }

            var flags = 0

            if (!key.consumeEvent) {
                flags = flags.withFlag(KeyCodeTriggerKeyEntity.FLAG_DO_NOT_CONSUME_KEY_EVENT)
            }

            return KeyCodeTriggerKeyEntity(
                keyCode = key.keyCode,
                deviceId = deviceId,
                deviceName = deviceName,
                clickType = clickType,
                flags = flags,
                uid = key.uid,
            )
        }
    }
}
