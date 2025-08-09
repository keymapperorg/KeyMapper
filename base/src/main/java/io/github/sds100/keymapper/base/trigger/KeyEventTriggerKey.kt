package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.common.utils.hasFlag
import io.github.sds100.keymapper.common.utils.withFlag
import io.github.sds100.keymapper.data.entities.KeyEventTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class KeyEventTriggerKey(
    override val uid: String = UUID.randomUUID().toString(),
  override  val keyCode: Int,
    val device: KeyEventTriggerDevice,
    override val clickType: ClickType,
    override val consumeEvent: Boolean = true,
    /**
     * Whether this key can only be detected by an input method. Some keys, such as DPAD buttons,
     * do not send key events to the accessibility service.
     */
    val requiresIme: Boolean = false,
   override val scanCode: Int? = null,
) : TriggerKey(), InputEventTriggerKey {

    override val allowedLongPress: Boolean = true
    override val allowedDoublePress: Boolean = true

    override fun toString(): String {
        val deviceString = when (device) {
            KeyEventTriggerDevice.Any -> "any"
            is KeyEventTriggerDevice.External -> "external"
            KeyEventTriggerDevice.Internal -> "internal"
        }
        return "InputEventTriggerKey(uid=${uid.substring(0..5)}, keyCode=$keyCode, device=$deviceString, clickType=$clickType, consume=$consumeEvent) "
    }

    // key code -> click type -> device -> consume key event
    override fun compareTo(other: TriggerKey) = when (other) {
        is KeyEventTriggerKey -> compareValuesBy(
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
        fun fromEntity(entity: KeyEventTriggerKeyEntity): TriggerKey {
            val device = when (entity.deviceId) {
                KeyEventTriggerKeyEntity.DEVICE_ID_THIS_DEVICE -> KeyEventTriggerDevice.Internal
                KeyEventTriggerKeyEntity.DEVICE_ID_ANY_DEVICE -> KeyEventTriggerDevice.Any
                else -> KeyEventTriggerDevice.External(
                    entity.deviceId,
                    entity.deviceName ?: "",
                )
            }

            val clickType = when (entity.clickType) {
                TriggerKeyEntity.SHORT_PRESS -> ClickType.SHORT_PRESS
                TriggerKeyEntity.LONG_PRESS -> ClickType.LONG_PRESS
                TriggerKeyEntity.DOUBLE_PRESS -> ClickType.DOUBLE_PRESS
                else -> ClickType.SHORT_PRESS
            }

            val consumeEvent =
                !entity.flags.hasFlag(KeyEventTriggerKeyEntity.FLAG_DO_NOT_CONSUME_KEY_EVENT)

            val requiresIme =
                entity.flags.hasFlag(KeyEventTriggerKeyEntity.FLAG_DETECTION_SOURCE_INPUT_METHOD)

            return KeyEventTriggerKey(
                uid = entity.uid,
                keyCode = entity.keyCode,
                device = device,
                clickType = clickType,
                consumeEvent = consumeEvent,
                requiresIme = requiresIme,
                scanCode = entity.scanCode
            )
        }

        fun toEntity(key: KeyEventTriggerKey): KeyEventTriggerKeyEntity {
            val deviceId = when (key.device) {
                KeyEventTriggerDevice.Any -> KeyEventTriggerKeyEntity.DEVICE_ID_ANY_DEVICE
                is KeyEventTriggerDevice.External -> key.device.descriptor
                KeyEventTriggerDevice.Internal -> KeyEventTriggerKeyEntity.DEVICE_ID_THIS_DEVICE
            }

            val deviceName =
                if (key.device is KeyEventTriggerDevice.External) {
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
                flags = flags.withFlag(KeyEventTriggerKeyEntity.FLAG_DO_NOT_CONSUME_KEY_EVENT)
            }

            if (key.requiresIme) {
                flags = flags.withFlag(KeyEventTriggerKeyEntity.FLAG_DETECTION_SOURCE_INPUT_METHOD)
            }

            return KeyEventTriggerKeyEntity(
                keyCode = key.keyCode,
                deviceId = deviceId,
                deviceName = deviceName,
                clickType = clickType,
                flags = flags,
                uid = key.uid,
                scanCode = key.scanCode
            )
        }
    }
}
