package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.floating.FloatingButtonData
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.system.inputevents.KeyEventUtils

/**
 * This extracts the core logic when configuring a trigger which makes it easier to write tests.
 */
class ConfigTriggerDelegate {

    fun addFloatingButtonTriggerKey(
        trigger: Trigger,
        buttonUid: String,
        button: FloatingButtonData?,
    ): Trigger {
        val clickType = when (trigger.mode) {
            is TriggerMode.Parallel -> trigger.mode.clickType
            TriggerMode.Sequence -> ClickType.SHORT_PRESS
            TriggerMode.Undefined -> ClickType.SHORT_PRESS
        }

        val triggerKey = FloatingButtonKey(
            buttonUid = buttonUid,
            button = button,
            clickType = clickType,
        )

        return addTriggerKey(trigger, triggerKey)
    }

    fun addAssistantTriggerKey(trigger: Trigger, type: AssistantTriggerType): Trigger {
        val clickType = when (trigger.mode) {
            is TriggerMode.Parallel -> trigger.mode.clickType
            TriggerMode.Sequence -> ClickType.SHORT_PRESS
            TriggerMode.Undefined -> ClickType.SHORT_PRESS
        }

        val triggerKey = AssistantTriggerKey(type = type, clickType = clickType)

        return addTriggerKey(trigger, triggerKey)
    }

    fun addFingerprintGesture(trigger: Trigger, type: FingerprintGestureType): Trigger {
        val clickType = when (trigger.mode) {
            is TriggerMode.Parallel -> trigger.mode.clickType
            TriggerMode.Sequence -> ClickType.SHORT_PRESS
            TriggerMode.Undefined -> ClickType.SHORT_PRESS
        }

        val triggerKey = FingerprintTriggerKey(type = type, clickType = clickType)

        return addTriggerKey(trigger, triggerKey)
    }

    /**
     * @param otherTriggerKeys This needs to check the other triggers in the app so that it can
     * enable scancode detection by default in some situations.
     */
    fun addKeyEventTriggerKey(
        trigger: Trigger,
        keyCode: Int,
        scanCode: Int,
        device: KeyEventTriggerDevice,
        requiresIme: Boolean,
        otherTriggerKeys: List<KeyCodeTriggerKey> = emptyList(),
    ): Trigger {
        val isPowerKey = KeyEventUtils.isPowerButtonKey(keyCode, scanCode)

        val clickType = if (isPowerKey) {
            ClickType.LONG_PRESS
        } else {
            when (trigger.mode) {
                is TriggerMode.Parallel -> trigger.mode.clickType
                TriggerMode.Sequence -> ClickType.SHORT_PRESS
                TriggerMode.Undefined -> ClickType.SHORT_PRESS
            }
        }

        var consumeKeyEvent = true

        // Issue #753
        if (KeyEventUtils.isModifierKey(keyCode)) {
            consumeKeyEvent = false
        }

        // Scan code detection should be turned on by default if there are other
        // keys from the same device that report the same key code but have a different scan code.
        val logicallyEqualKeys = otherTriggerKeys.plus(trigger.keys)
            .filterIsInstance<KeyEventTriggerKey>()
            // Assume that keys without a scan code come from the same device so ignore them.
            // The scan code was not saved on versions older than 4.0
            .filter { it.scanCode != null }
            .filter {
                it.keyCode == keyCode &&
                    it.scanCode != scanCode &&
                    it.device == device
            }

        val triggerKey = KeyEventTriggerKey(
            keyCode = keyCode,
            device = device,
            clickType = clickType,
            scanCode = scanCode,
            consumeEvent = consumeKeyEvent,
            requiresIme = requiresIme,
            detectWithScanCodeUserSetting = logicallyEqualKeys.isNotEmpty(),
        )

        var newKeys = trigger.keys.filter { it !is EvdevTriggerKey }

        if (isPowerKey && trigger.mode is TriggerMode.Parallel) {
            newKeys = newKeys.map { it.setClickType(ClickType.LONG_PRESS) }
        }

        val newMode = if (isPowerKey && trigger.mode is TriggerMode.Parallel) {
            TriggerMode.Parallel(ClickType.LONG_PRESS)
        } else {
            trigger.mode
        }

        return addTriggerKey(trigger.copy(mode = newMode, keys = newKeys), triggerKey)
    }

    fun addEvdevTriggerKey(
        trigger: Trigger,
        keyCode: Int,
        scanCode: Int,
        device: EvdevDeviceInfo,
        otherTriggerKeys: List<KeyCodeTriggerKey> = emptyList(),
    ): Trigger {
        val isPowerKey = KeyEventUtils.isPowerButtonKey(keyCode, scanCode)

        val clickType = if (isPowerKey) {
            ClickType.LONG_PRESS
        } else {
            when (trigger.mode) {
                is TriggerMode.Parallel -> trigger.mode.clickType
                TriggerMode.Sequence -> ClickType.SHORT_PRESS
                TriggerMode.Undefined -> ClickType.SHORT_PRESS
            }
        }

        // Scan code detection should be turned on by default if there are other
        // keys from the same device that report the same key code but have a different scan code.
        val conflictingKeys = otherTriggerKeys.plus(trigger.keys)
            .filterIsInstance<EvdevTriggerKey>()
            .filter {
                it.keyCode == keyCode &&
                    it.scanCode != scanCode &&
                    it.device == device
            }

        val triggerKey = EvdevTriggerKey(
            keyCode = keyCode,
            scanCode = scanCode,
            device = device,
            clickType = clickType,
            consumeEvent = true,
            detectWithScanCodeUserSetting = conflictingKeys.isNotEmpty(),
        )

        var newKeys = trigger.keys.filter { it !is KeyEventTriggerKey }

        if (isPowerKey && trigger.mode is TriggerMode.Parallel) {
            newKeys = newKeys.map { it.setClickType(ClickType.LONG_PRESS) }
        }

        val newMode = if (isPowerKey && trigger.mode is TriggerMode.Parallel) {
            TriggerMode.Parallel(ClickType.LONG_PRESS)
        } else {
            trigger.mode
        }

        return addTriggerKey(trigger.copy(mode = newMode, keys = newKeys), triggerKey)
    }

    private fun addTriggerKey(trigger: Trigger, key: TriggerKey): Trigger {
        // Check whether the trigger already contains the key because if so
        // then it must be converted to a sequence trigger.
        val containsKey = trigger.keys.any { otherKey -> key.isLogicallyEqual(otherKey) }

        var newKeys: List<TriggerKey> = trigger.keys.plus(key)

        val newMode = when {
            trigger.mode != TriggerMode.Sequence && containsKey -> TriggerMode.Sequence
            newKeys.size <= 1 -> TriggerMode.Undefined

            /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
            because this is what most users are expecting when they make a trigger with multiple keys */
            newKeys.size == 2 && !containsKey -> {
                newKeys = newKeys.map { it.setClickType(key.clickType) }
                TriggerMode.Parallel(key.clickType)
            }

            else -> trigger.mode
        }

        return trigger.copy(keys = newKeys, mode = newMode).validate()
    }

    fun removeTriggerKey(trigger: Trigger, uid: String): Trigger {
        val newKeys = trigger.keys.toMutableList().apply {
            removeAll { it.uid == uid }
        }

        val newMode = when {
            newKeys.size <= 1 -> TriggerMode.Undefined
            else -> trigger.mode
        }

        return trigger.copy(keys = newKeys, mode = newMode).validate()
    }

    fun moveTriggerKey(trigger: Trigger, fromIndex: Int, toIndex: Int): Trigger {
        // Don't need to validate. This should be low latency so moving is responsive.
        return trigger.copy(
            keys = trigger.keys.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            },
        )
    }

    fun setParallelTriggerMode(trigger: Trigger): Trigger {
        if (trigger.mode is TriggerMode.Parallel) {
            return trigger
        }

        // undefined mode only allowed if one or no keys
        if (trigger.keys.size <= 1) {
            return trigger.copy(mode = TriggerMode.Undefined)
        }

        val oldKeys = trigger.keys
        val newKeys: MutableList<TriggerKey> = mutableListOf()

        // In a parallel trigger keys must be triggered by different key events
        outerLoop@ for (key in oldKeys) {
            for (other in newKeys) {
                if (key.isLogicallyEqual(other)) {
                    continue@outerLoop
                }
            }

            // set all the keys to a short press if coming from a non-parallel trigger
            // because they must all be the same click type and can't all be double pressed
            newKeys.add(key.setClickType(ClickType.SHORT_PRESS))
        }

        return trigger
            .copy(keys = newKeys, mode = TriggerMode.Parallel(ClickType.SHORT_PRESS))
            .validate()
    }

    fun setSequenceTriggerMode(trigger: Trigger): Trigger {
        if (trigger.mode == TriggerMode.Sequence) return trigger
        // undefined mode only allowed if one or no keys
        if (trigger.keys.size <= 1) {
            return trigger.copy(mode = TriggerMode.Undefined)
        }

        return trigger.copy(mode = TriggerMode.Sequence).validate()
    }

    fun setUndefinedTriggerMode(trigger: Trigger): Trigger {
        if (trigger.mode == TriggerMode.Undefined) return trigger

        // undefined mode only allowed if one or no keys
        if (trigger.keys.size > 1) {
            return trigger
        }

        return trigger.copy(mode = TriggerMode.Undefined).validate()
    }

    fun setTriggerShortPress(trigger: Trigger): Trigger {
        if (trigger.mode == TriggerMode.Sequence) {
            return trigger
        }

        val newKeys = trigger.keys.map { it.setClickType(clickType = ClickType.SHORT_PRESS) }
        val newMode = if (newKeys.size <= 1) {
            TriggerMode.Undefined
        } else {
            TriggerMode.Parallel(ClickType.SHORT_PRESS)
        }
        return trigger.copy(keys = newKeys, mode = newMode).validate()
    }

    fun setTriggerLongPress(trigger: Trigger): Trigger {
        if (trigger.mode == TriggerMode.Sequence) {
            return trigger
        }

        // You can't set the trigger to a long press if it contains a key
        // that isn't detected with key codes. This is because there aren't
        // separate key events for the up and down press that can be timed.
        if (trigger.keys.any { !it.allowedLongPress }) {
            return trigger
        }

        val newKeys = trigger.keys.map { it.setClickType(clickType = ClickType.LONG_PRESS) }
        val newMode = if (newKeys.size <= 1) {
            TriggerMode.Undefined
        } else {
            TriggerMode.Parallel(ClickType.LONG_PRESS)
        }

        return trigger.copy(keys = newKeys, mode = newMode).validate()
    }

    fun setTriggerDoublePress(trigger: Trigger): Trigger {
        if (trigger.mode != TriggerMode.Undefined) {
            return trigger
        }

        if (trigger.keys.any { !it.allowedDoublePress }) {
            return trigger
        }

        val newKeys = trigger.keys.map { it.setClickType(clickType = ClickType.DOUBLE_PRESS) }
        val newMode = TriggerMode.Undefined

        return trigger.copy(keys = newKeys, mode = newMode).validate()
    }

    fun setTriggerKeyClickType(trigger: Trigger, keyUid: String, clickType: ClickType): Trigger {
        val newKeys = trigger.keys.map {
            if (it.uid == keyUid) {
                it.setClickType(clickType = clickType)
            } else {
                it
            }
        }

        return trigger.copy(keys = newKeys).validate()
    }

    fun setTriggerKeyDevice(
        trigger: Trigger,
        keyUid: String,
        device: KeyEventTriggerDevice,
    ): Trigger {
        val newKeys = trigger.keys.map { key ->
            if (key.uid == keyUid) {
                if (key !is KeyEventTriggerKey) {
                    throw IllegalArgumentException(
                        "You can not set the device for non KeyEventTriggerKeys.",
                    )
                }

                key.copy(device = device)
            } else {
                key
            }
        }

        return trigger.copy(keys = newKeys).validate()
    }

    fun setTriggerKeyConsumeKeyEvent(
        trigger: Trigger,
        keyUid: String,
        consumeKeyEvent: Boolean,
    ): Trigger {
        val newKeys = trigger.keys.map { key ->
            if (key.uid == keyUid) {
                when (key) {
                    is KeyEventTriggerKey -> {
                        key.copy(consumeEvent = consumeKeyEvent)
                    }

                    is EvdevTriggerKey -> {
                        key.copy(consumeEvent = consumeKeyEvent)
                    }

                    else -> {
                        key
                    }
                }
            } else {
                key
            }
        }

        return trigger.copy(keys = newKeys).validate()
    }

    fun setAssistantTriggerKeyType(
        trigger: Trigger,
        keyUid: String,
        type: AssistantTriggerType,
    ): Trigger {
        val newKeys = trigger.keys.map { key ->
            if (key.uid == keyUid) {
                if (key is AssistantTriggerKey) {
                    key.copy(type = type)
                } else {
                    key
                }
            } else {
                key
            }
        }

        return trigger.copy(keys = newKeys).validate()
    }

    fun setFingerprintGestureType(
        trigger: Trigger,
        keyUid: String,
        type: FingerprintGestureType,
    ): Trigger {
        val newKeys = trigger.keys.map { key ->
            if (key.uid == keyUid) {
                if (key is FingerprintTriggerKey) {
                    key.copy(type = type)
                } else {
                    key
                }
            } else {
                key
            }
        }

        return trigger.copy(keys = newKeys).validate()
    }

    fun setVibrateEnabled(trigger: Trigger, enabled: Boolean): Trigger {
        return trigger.copy(vibrate = enabled).validate()
    }

    fun setVibrationDuration(
        trigger: Trigger,
        duration: Int,
        defaultVibrateDuration: Int,
    ): Trigger {
        return if (duration == defaultVibrateDuration) {
            trigger.copy(vibrateDuration = null).validate()
        } else {
            trigger.copy(vibrateDuration = duration).validate()
        }
    }

    fun setLongPressDelay(trigger: Trigger, delay: Int, defaultLongPressDelay: Int): Trigger {
        return if (delay == defaultLongPressDelay) {
            trigger.copy(longPressDelay = null).validate()
        } else {
            trigger.copy(longPressDelay = delay).validate()
        }
    }

    fun setDoublePressDelay(trigger: Trigger, delay: Int, defaultDoublePressDelay: Int): Trigger {
        return if (delay == defaultDoublePressDelay) {
            trigger.copy(doublePressDelay = null).validate()
        } else {
            trigger.copy(doublePressDelay = delay).validate()
        }
    }

    fun setSequenceTriggerTimeout(
        trigger: Trigger,
        delay: Int,
        defaultSequenceTriggerTimeout: Int,
    ): Trigger {
        return if (delay == defaultSequenceTriggerTimeout) {
            trigger.copy(sequenceTriggerTimeout = null).validate()
        } else {
            trigger.copy(sequenceTriggerTimeout = delay).validate()
        }
    }

    fun setLongPressDoubleVibrationEnabled(trigger: Trigger, enabled: Boolean): Trigger {
        return trigger.copy(longPressDoubleVibration = enabled).validate()
    }

    fun setTriggerFromOtherAppsEnabled(trigger: Trigger, enabled: Boolean): Trigger {
        return trigger.copy(triggerFromOtherApps = enabled).validate()
    }

    fun setShowToastEnabled(trigger: Trigger, enabled: Boolean): Trigger {
        return trigger.copy(showToast = enabled).validate()
    }

    fun setScanCodeDetectionEnabled(trigger: Trigger, keyUid: String, enabled: Boolean): Trigger {
        val newKeys = trigger.keys.map { otherKey ->
            if (otherKey.uid == keyUid &&
                otherKey is KeyCodeTriggerKey &&
                otherKey.isScanCodeDetectionUserConfigurable()
            ) {
                when (otherKey) {
                    is KeyEventTriggerKey -> {
                        otherKey.copy(detectWithScanCodeUserSetting = enabled)
                    }

                    is EvdevTriggerKey -> {
                        otherKey.copy(detectWithScanCodeUserSetting = enabled)
                    }
                }
            } else {
                otherKey
            }
        }

        return trigger.copy(keys = newKeys).validate()
    }
}
