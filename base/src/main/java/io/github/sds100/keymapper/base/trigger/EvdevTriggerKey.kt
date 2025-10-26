package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.hasFlag
import io.github.sds100.keymapper.common.utils.withFlag
import io.github.sds100.keymapper.data.entities.EvdevTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity
import java.util.UUID

/**
 * This must be a different class to KeyEventTriggerKey because trigger keys from evdev events
 * must come from one device, even if it is internal, and can not come from any device. The input
 * devices must be grabbed so that Key Mapper can remap them.
 */
data class EvdevTriggerKey(
    override val uid: String = UUID.randomUUID().toString(),
    override val keyCode: Int,
    override val scanCode: Int,
    val device: EvdevDeviceInfo,
    override val clickType: ClickType = ClickType.SHORT_PRESS,
    override val consumeEvent: Boolean = true,
    override val detectWithScanCodeUserSetting: Boolean = false,
) : TriggerKey(),
    KeyCodeTriggerKey {
    override val allowedDoublePress: Boolean = true
    override val allowedLongPress: Boolean = true

    override fun isSameDevice(otherKey: KeyCodeTriggerKey): Boolean {
        if (otherKey !is EvdevTriggerKey) {
            return false
        }
        return device == otherKey.device
    }

    companion object {
        fun fromEntity(entity: EvdevTriggerKeyEntity): TriggerKey {
            val clickType =
                when (entity.clickType) {
                    TriggerKeyEntity.SHORT_PRESS -> ClickType.SHORT_PRESS
                    TriggerKeyEntity.LONG_PRESS -> ClickType.LONG_PRESS
                    TriggerKeyEntity.DOUBLE_PRESS -> ClickType.DOUBLE_PRESS
                    else -> ClickType.SHORT_PRESS
                }

            val consumeEvent =
                !entity.flags.hasFlag(EvdevTriggerKeyEntity.FLAG_DO_NOT_CONSUME_KEY_EVENT)

            val detectWithScancode =
                entity.flags.hasFlag(EvdevTriggerKeyEntity.FLAG_DETECT_WITH_SCAN_CODE)

            return EvdevTriggerKey(
                uid = entity.uid,
                keyCode = entity.keyCode,
                scanCode = entity.scanCode,
                device =
                    EvdevDeviceInfo(
                        name = entity.deviceName,
                        bus = entity.deviceBus,
                        vendor = entity.deviceVendor,
                        product = entity.deviceProduct,
                    ),
                clickType = clickType,
                consumeEvent = consumeEvent,
                detectWithScanCodeUserSetting = detectWithScancode,
            )
        }

        fun toEntity(key: EvdevTriggerKey): EvdevTriggerKeyEntity {
            val clickType =
                when (key.clickType) {
                    ClickType.SHORT_PRESS -> TriggerKeyEntity.SHORT_PRESS
                    ClickType.LONG_PRESS -> TriggerKeyEntity.LONG_PRESS
                    ClickType.DOUBLE_PRESS -> TriggerKeyEntity.DOUBLE_PRESS
                }

            var flags = 0

            if (!key.consumeEvent) {
                flags = flags.withFlag(EvdevTriggerKeyEntity.FLAG_DO_NOT_CONSUME_KEY_EVENT)
            }

            if (key.detectWithScanCodeUserSetting) {
                flags = flags.withFlag(EvdevTriggerKeyEntity.FLAG_DETECT_WITH_SCAN_CODE)
            }

            return EvdevTriggerKeyEntity(
                keyCode = key.keyCode,
                scanCode = key.scanCode,
                deviceName = key.device.name,
                deviceBus = key.device.bus,
                deviceVendor = key.device.vendor,
                deviceProduct = key.device.product,
                clickType = clickType,
                flags = flags,
                uid = key.uid,
            )
        }
    }
}
