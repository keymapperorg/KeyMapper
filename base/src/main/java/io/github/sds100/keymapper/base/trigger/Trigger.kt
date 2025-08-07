package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.floating.FloatingButtonEntityMapper
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.common.utils.hasFlag
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.common.utils.withFlag
import io.github.sds100.keymapper.data.entities.AssistantTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.EntityExtra
import io.github.sds100.keymapper.data.entities.EvdevTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.FingerprintTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.FloatingButtonEntityWithLayout
import io.github.sds100.keymapper.data.entities.FloatingButtonKeyEntity
import io.github.sds100.keymapper.data.entities.KeyEventTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.TriggerEntity
import io.github.sds100.keymapper.data.entities.getData
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import kotlinx.serialization.Serializable

@Serializable
data class Trigger(
    val keys: List<TriggerKey> = emptyList(),
    val mode: TriggerMode = TriggerMode.Undefined,
    val vibrate: Boolean = false,
    val longPressDoubleVibration: Boolean = false,
    val screenOffTrigger: Boolean = false,
    val longPressDelay: Int? = null,
    val doublePressDelay: Int? = null,
    val vibrateDuration: Int? = null,
    val sequenceTriggerTimeout: Int? = null,
    val triggerFromOtherApps: Boolean = false,
    val showToast: Boolean = false,
) {
    fun isVibrateAllowed(): Boolean = true

    fun isChangingVibrationDurationAllowed(): Boolean = vibrate || longPressDoubleVibration

    fun isChangingLongPressDelayAllowed(): Boolean = keys.any { key -> key.clickType == ClickType.LONG_PRESS }

    fun isChangingDoublePressDelayAllowed(): Boolean = keys.any { key -> key.clickType == ClickType.DOUBLE_PRESS }

    fun isLongPressDoubleVibrationAllowed(): Boolean = (keys.size == 1 || (mode is TriggerMode.Parallel)) &&
        keys.getOrNull(0)?.clickType == ClickType.LONG_PRESS

    /**
     * Must check that it is not empty otherwise it would be true from the "all" check.
     * It is not allowed if the key is an assistant button because it is assumed to be true
     * anyway.
     */
    fun isDetectingWhenScreenOffAllowed(): Boolean {
        return keys.isNotEmpty() &&
            keys.all {
                it is KeyEventTriggerKey &&
                    InputEventUtils.canDetectKeyWhenScreenOff(
                        it.keyCode,
                    )
            }
    }

    fun isChangingSequenceTriggerTimeoutAllowed(): Boolean = keys.isNotEmpty() && keys.size > 1 && mode is TriggerMode.Sequence

    fun updateFloatingButtonData(buttons: List<FloatingButtonEntityWithLayout>): Trigger {
        val newTriggerKeys = keys.map { key ->
            if (key is FloatingButtonKey) {
                val buttonLayout = buttons.find { it.button.uid == key.buttonUid }

                if (buttonLayout == null) {
                    key.copy(button = null)
                } else {
                    val buttonData = FloatingButtonEntityMapper.fromEntity(
                        buttonLayout.button,
                        buttonLayout.layout.name,
                    )
                    key.copy(button = buttonData)
                }
            } else {
                key
            }
        }

        return copy(keys = newTriggerKeys)
    }
}

object TriggerEntityMapper {
    fun fromEntity(
        entity: TriggerEntity,
        floatingButtons: List<FloatingButtonEntityWithLayout>,
    ): Trigger {
        val keys = entity.keys.map { key ->
            when (key) {
                is AssistantTriggerKeyEntity -> AssistantTriggerKey.fromEntity(key)
                is KeyEventTriggerKeyEntity -> KeyEventTriggerKey.fromEntity(
                    key,
                )
                is FloatingButtonKeyEntity -> {
                    val floatingButton = floatingButtons.find { it.button.uid == key.buttonUid }
                    FloatingButtonKey.fromEntity(key, floatingButton)
                }

                is FingerprintTriggerKeyEntity -> FingerprintTriggerKey.fromEntity(key)
                is EvdevTriggerKeyEntity -> EvdevTriggerKey.fromEntity(key)
            }
        }

        val mode = when {
            entity.mode == TriggerEntity.SEQUENCE && keys.size > 1 -> TriggerMode.Sequence
            entity.mode == TriggerEntity.PARALLEL && keys.size > 1 -> TriggerMode.Parallel(keys[0].clickType)
            else -> TriggerMode.Undefined
        }

        return Trigger(
            keys = keys,
            mode = mode,

            vibrate = entity.flags.hasFlag(TriggerEntity.TRIGGER_FLAG_VIBRATE),

            longPressDoubleVibration =
            entity.flags.hasFlag(TriggerEntity.TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION),

            longPressDelay = entity.extras.getData(TriggerEntity.EXTRA_LONG_PRESS_DELAY)
                .valueOrNull()?.toIntOrNull(),

            doublePressDelay = entity.extras.getData(TriggerEntity.EXTRA_DOUBLE_PRESS_DELAY)
                .valueOrNull()?.toIntOrNull(),

            vibrateDuration = entity.extras.getData(TriggerEntity.EXTRA_VIBRATION_DURATION)
                .valueOrNull()?.toIntOrNull(),

            sequenceTriggerTimeout = entity.extras.getData(TriggerEntity.EXTRA_SEQUENCE_TRIGGER_TIMEOUT)
                .valueOrNull()?.toIntOrNull(),

            triggerFromOtherApps = entity.flags.hasFlag(TriggerEntity.TRIGGER_FLAG_FROM_OTHER_APPS),
            showToast = entity.flags.hasFlag(TriggerEntity.TRIGGER_FLAG_SHOW_TOAST),
            screenOffTrigger = entity.flags.hasFlag(TriggerEntity.TRIGGER_FLAG_SCREEN_OFF_TRIGGERS),
        )
    }

    fun toEntity(trigger: Trigger): TriggerEntity {
        val extras = mutableListOf<EntityExtra>()

        if (trigger.isChangingSequenceTriggerTimeoutAllowed() && trigger.sequenceTriggerTimeout != null) {
            extras.add(
                EntityExtra(
                    TriggerEntity.EXTRA_SEQUENCE_TRIGGER_TIMEOUT,
                    trigger.sequenceTriggerTimeout.toString(),
                ),
            )
        }

        if (trigger.isChangingLongPressDelayAllowed() && trigger.longPressDelay != null) {
            extras.add(
                EntityExtra(
                    TriggerEntity.EXTRA_LONG_PRESS_DELAY,
                    trigger.longPressDelay.toString(),
                ),
            )
        }

        if (trigger.isChangingDoublePressDelayAllowed() && trigger.doublePressDelay != null) {
            extras.add(
                EntityExtra(
                    TriggerEntity.EXTRA_DOUBLE_PRESS_DELAY,
                    trigger.doublePressDelay.toString(),
                ),
            )
        }

        if (trigger.isChangingVibrationDurationAllowed() && trigger.vibrateDuration != null) {
            extras.add(
                EntityExtra(
                    TriggerEntity.EXTRA_VIBRATION_DURATION,
                    trigger.vibrateDuration.toString(),
                ),
            )
        }

        val mode = when (trigger.mode) {
            is TriggerMode.Parallel -> TriggerEntity.PARALLEL
            TriggerMode.Sequence -> TriggerEntity.SEQUENCE
            TriggerMode.Undefined -> TriggerEntity.UNDEFINED
        }

        var flags = 0

        if (trigger.isVibrateAllowed() && trigger.vibrate) {
            flags = flags.withFlag(TriggerEntity.TRIGGER_FLAG_VIBRATE)
        }

        if (trigger.isLongPressDoubleVibrationAllowed() && trigger.longPressDoubleVibration) {
            flags = flags.withFlag(TriggerEntity.TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION)
        }

        if (trigger.isDetectingWhenScreenOffAllowed() && trigger.screenOffTrigger) {
            flags = flags.withFlag(TriggerEntity.TRIGGER_FLAG_SCREEN_OFF_TRIGGERS)
        }

        if (trigger.triggerFromOtherApps) {
            flags = flags.withFlag(TriggerEntity.TRIGGER_FLAG_FROM_OTHER_APPS)
        }

        if (trigger.showToast) {
            flags = flags.withFlag(TriggerEntity.TRIGGER_FLAG_SHOW_TOAST)
        }

        val keys = trigger.keys.map { key ->
            when (key) {
                is AssistantTriggerKey -> AssistantTriggerKey.toEntity(key)
                is KeyEventTriggerKey -> KeyEventTriggerKey.toEntity(
                    key,
                )
                is FloatingButtonKey -> FloatingButtonKey.toEntity(key)
                is FingerprintTriggerKey -> FingerprintTriggerKey.toEntity(key)
                is EvdevTriggerKey -> EvdevTriggerKey.toEntity(key)
            }
        }

        return TriggerEntity(
            keys = keys,
            extras = extras,
            mode = mode,
            flags = flags,
        )
    }
}
