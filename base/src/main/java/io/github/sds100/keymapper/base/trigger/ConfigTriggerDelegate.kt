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

        // Check whether the trigger already contains the key because if so
        // then it must be converted to a sequence trigger.
        val containsKey = trigger.keys
            .filterIsInstance<FloatingButtonKey>()
            .any { keyToCompare -> keyToCompare.buttonUid == buttonUid }

        val triggerKey = FloatingButtonKey(
            buttonUid = buttonUid,
            button = button,
            clickType = clickType,
        )

        var newKeys = trigger.keys.plus(triggerKey)

        val newMode = when {
            trigger.mode != TriggerMode.Sequence && containsKey -> TriggerMode.Sequence
            newKeys.size <= 1 -> TriggerMode.Undefined

            /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
                because this is what most users are expecting when they make a trigger with multiple keys */
            newKeys.size == 2 && !containsKey -> {
                newKeys = newKeys.map { it.setClickType(triggerKey.clickType) }
                TriggerMode.Parallel(triggerKey.clickType)
            }

            else -> trigger.mode
        }

        return trigger.copy(keys = newKeys, mode = newMode)
    }


    fun addAssistantTriggerKey(trigger: Trigger, type: AssistantTriggerType): Trigger {
        val clickType = when (trigger.mode) {
            is TriggerMode.Parallel -> trigger.mode.clickType
            TriggerMode.Sequence -> ClickType.SHORT_PRESS
            TriggerMode.Undefined -> ClickType.SHORT_PRESS
        }

        // Check whether the trigger already contains the key because if so
        // then it must be converted to a sequence trigger.
        val containsAssistantKey = trigger.keys.any { it is AssistantTriggerKey }

        val triggerKey = AssistantTriggerKey(type = type, clickType = clickType)

        val newKeys = trigger.keys.plus(triggerKey).map { it.setClickType(ClickType.SHORT_PRESS) }

        val newMode = when {
            trigger.mode != TriggerMode.Sequence && containsAssistantKey -> TriggerMode.Sequence
            newKeys.size <= 1 -> TriggerMode.Undefined

            /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
            because this is what most users are expecting when they make a trigger with multiple keys.

            It must be a short press because long pressing the assistant key isn't supported.
             */
            !containsAssistantKey -> TriggerMode.Parallel(ClickType.SHORT_PRESS)
            else -> trigger.mode
        }

        return trigger.copy(keys = newKeys, mode = newMode)
    }

    fun addFingerprintGesture(trigger: Trigger, type: FingerprintGestureType): Trigger {
        val clickType = when (trigger.mode) {
            is TriggerMode.Parallel -> trigger.mode.clickType
            TriggerMode.Sequence -> ClickType.SHORT_PRESS
            TriggerMode.Undefined -> ClickType.SHORT_PRESS
        }

        // Check whether the trigger already contains the key because if so
        // then it must be converted to a sequence trigger.
        val containsFingerprintGesture = trigger.keys.any { it is FingerprintTriggerKey }

        val triggerKey = FingerprintTriggerKey(type = type, clickType = clickType)

        val newKeys = trigger.keys.plus(triggerKey).map { it.setClickType(ClickType.SHORT_PRESS) }

        val newMode = when {
            trigger.mode != TriggerMode.Sequence && containsFingerprintGesture -> TriggerMode.Sequence
            newKeys.size <= 1 -> TriggerMode.Undefined

            /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
            because this is what most users are expecting when they make a trigger with multiple keys.

            It must be a short press because long pressing the assistant key isn't supported.
             */
            !containsFingerprintGesture -> TriggerMode.Parallel(ClickType.SHORT_PRESS)
            else -> trigger.mode
        }

        return trigger.copy(keys = newKeys, mode = newMode)
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
        otherTriggerKeys: List<KeyCodeTriggerKey> = emptyList()
    ): Trigger {
        val clickType = when (trigger.mode) {
            is TriggerMode.Parallel -> trigger.mode.clickType
            TriggerMode.Sequence -> ClickType.SHORT_PRESS
            TriggerMode.Undefined -> ClickType.SHORT_PRESS
        }

        // Check whether the trigger already contains the key because if so
        // then it must be converted to a sequence trigger.
        val containsKey = trigger.keys
            .filterIsInstance<KeyEventTriggerKey>()
            .any { keyToCompare ->
                keyToCompare.keyCode == keyCode && keyToCompare.device.isSameDevice(device)
            }

        var consumeKeyEvent = true

        // Issue #753
        if (KeyEventUtils.isModifierKey(keyCode)) {
            consumeKeyEvent = false
        }

        // Scan code detection should be turned on by default if there are other
        // keys from the same device that report the same key code but have a different scan code.
        val conflictingKeys = otherTriggerKeys.plus(trigger.keys)
            .filterIsInstance<KeyEventTriggerKey>()
            .filter { it.isConflictingKey(keyCode, scanCode, device) }

        val triggerKey = KeyEventTriggerKey(
            keyCode = keyCode,
            device = device,
            clickType = clickType,
            scanCode = scanCode,
            consumeEvent = consumeKeyEvent,
            requiresIme = requiresIme,
            detectWithScanCodeUserSetting = conflictingKeys.isNotEmpty()
        )

        var newKeys = trigger.keys.filter { it !is EvdevTriggerKey }.plus(triggerKey)

        val newMode = when {
            trigger.mode != TriggerMode.Sequence && containsKey -> TriggerMode.Sequence
            newKeys.size <= 1 -> TriggerMode.Undefined

            /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
            because this is what most users are expecting when they make a trigger with multiple keys */
            newKeys.size == 2 && !containsKey -> {
                newKeys = newKeys.map { it.setClickType(triggerKey.clickType) }
                TriggerMode.Parallel(triggerKey.clickType)
            }

            else -> trigger.mode
        }

        return trigger.copy(keys = newKeys, mode = newMode)
    }

    /**
     * This will return true if the key has same key code but different
     * scan code, and is from the same device.
     */
    private fun KeyEventTriggerKey.isConflictingKey(
        keyCode: Int,
        scanCode: Int,
        device: KeyEventTriggerDevice,
    ): Boolean {
        return this.keyCode == keyCode
            && this.scanCode != scanCode
            && this.device.isSameDevice(device)
    }

    fun addEvdevTriggerKey(
        trigger: Trigger,
        keyCode: Int,
        scanCode: Int,
        device: EvdevDeviceInfo,
        otherTriggerKeys: List<KeyCodeTriggerKey> = emptyList()
    ): Trigger {
        val clickType = when (trigger.mode) {
            is TriggerMode.Parallel -> trigger.mode.clickType
            TriggerMode.Sequence -> ClickType.SHORT_PRESS
            TriggerMode.Undefined -> ClickType.SHORT_PRESS
        }

        // Check whether the trigger already contains the key because if so
        // then it must be converted to a sequence trigger.
        val containsKey = trigger.keys
            .filterIsInstance<EvdevTriggerKey>()
            .any { keyToCompare ->
                keyToCompare.keyCode == keyCode && keyToCompare.device == device
            }

        // Scan code detection should be turned on by default if there are other
        // keys from the same device that report the same key code but have a different scan code.
        val conflictingKeys = otherTriggerKeys.plus(trigger.keys)
            .filterIsInstance<EvdevTriggerKey>()
            .filter { it.isConflictingKey(keyCode, scanCode, device) }

        val triggerKey = EvdevTriggerKey(
            keyCode = keyCode,
            scanCode = scanCode,
            device = device,
            clickType = clickType,
            consumeEvent = true,
            detectWithScanCodeUserSetting = conflictingKeys.isNotEmpty()
        )

        var newKeys = trigger.keys.filter { it !is KeyEventTriggerKey }.plus(triggerKey)

        val newMode = when {
            trigger.mode != TriggerMode.Sequence && containsKey -> TriggerMode.Sequence
            newKeys.size <= 1 -> TriggerMode.Undefined

            /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
            because this is what most users are expecting when they make a trigger with multiple keys */
            newKeys.size == 2 && !containsKey -> {
                newKeys = newKeys.map { it.setClickType(triggerKey.clickType) }
                TriggerMode.Parallel(triggerKey.clickType)
            }

            else -> trigger.mode
        }

        return trigger.copy(keys = newKeys, mode = newMode)
    }

    /**
     * This will return true if the key has same key code but different
     * scan code, and is from the same device.
     */
    private fun EvdevTriggerKey.isConflictingKey(
        keyCode: Int,
        scanCode: Int,
        device: EvdevDeviceInfo,
    ): Boolean {
        return this.keyCode == keyCode
            && this.scanCode != scanCode
            && this.device == device
    }

    fun removeTriggerKey(trigger: Trigger, uid: String): Trigger {
        val newKeys = trigger.keys.toMutableList().apply {
            removeAll { it.uid == uid }
        }

        val newMode = when {
            newKeys.size <= 1 -> TriggerMode.Undefined
            else -> trigger.mode
        }

        return trigger.copy(keys = newKeys, mode = newMode)
    }

    fun moveTriggerKey(trigger: Trigger, fromIndex: Int, toIndex: Int): Trigger {
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
        var newKeys = oldKeys

        // set all the keys to a short press if coming from a non-parallel trigger
        // because they must all be the same click type and can't all be double pressed
        newKeys = newKeys
            .map { key -> key.setClickType(clickType = ClickType.SHORT_PRESS) }
            // remove duplicates of keys that have the same keycode and device id
            .distinctBy { key ->
                when (key) {
                    // You can't mix assistant trigger types in a parallel trigger because there is no notion of a "down" key event, which means they can't be pressed at the same time
                    is AssistantTriggerKey, is FingerprintTriggerKey -> 0
                    is KeyEventTriggerKey -> Pair(
                        key.keyCode,
                        key.device,
                    )

                    is FloatingButtonKey -> key.buttonUid
                    is EvdevTriggerKey -> Pair(
                        key.keyCode,
                        key.device,
                    )
                }
            }

        val newMode = if (newKeys.size <= 1) {
            TriggerMode.Undefined
        } else {
            TriggerMode.Parallel(newKeys[0].clickType)
        }

        return trigger.copy(keys = newKeys, mode = newMode)
    }

    fun setSequenceTriggerMode(trigger: Trigger): Trigger {
        if (trigger.mode == TriggerMode.Sequence) return trigger
        // undefined mode only allowed if one or no keys
        if (trigger.keys.size <= 1) {
            return trigger.copy(mode = TriggerMode.Undefined)
        }

        return trigger.copy(mode = TriggerMode.Sequence)
    }

    fun setUndefinedTriggerMode(trigger: Trigger): Trigger {
        if (trigger.mode == TriggerMode.Undefined) return trigger

        // undefined mode only allowed if one or no keys
        if (trigger.keys.size > 1) {
            return trigger
        }

        return trigger.copy(mode = TriggerMode.Undefined)
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
        return trigger.copy(keys = newKeys, mode = newMode)
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

        return trigger.copy(keys = newKeys, mode = newMode)
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

        return trigger.copy(keys = newKeys, mode = newMode)
    }

    fun setTriggerKeyClickType(trigger: Trigger, keyUid: String, clickType: ClickType): Trigger {
        val newKeys = trigger.keys.map {
            if (it.uid == keyUid) {
                it.setClickType(clickType = clickType)
            } else {
                it
            }
        }

        return trigger.copy(keys = newKeys)
    }

    fun setTriggerKeyDevice(
        trigger: Trigger,
        keyUid: String,
        device: KeyEventTriggerDevice
    ): Trigger {
        val newKeys = trigger.keys.map { key ->
            if (key.uid == keyUid) {
                if (key !is KeyEventTriggerKey) {
                    throw IllegalArgumentException("You can not set the device for non KeyEventTriggerKeys.")
                }

                key.copy(device = device)
            } else {
                key
            }
        }

        return trigger.copy(keys = newKeys)
    }

    fun setTriggerKeyConsumeKeyEvent(
        trigger: Trigger,
        keyUid: String,
        consumeKeyEvent: Boolean
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

        return trigger.copy(keys = newKeys)
    }

    fun setAssistantTriggerKeyType(
        trigger: Trigger,
        keyUid: String,
        type: AssistantTriggerType
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

        return trigger.copy(keys = newKeys)
    }

    fun setFingerprintGestureType(
        trigger: Trigger,
        keyUid: String,
        type: FingerprintGestureType
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

        return trigger.copy(keys = newKeys)
    }

    fun setVibrateEnabled(trigger: Trigger, enabled: Boolean): Trigger {
        return trigger.copy(vibrate = enabled)
    }

    fun setVibrationDuration(
        trigger: Trigger,
        duration: Int,
        defaultVibrateDuration: Int
    ): Trigger {
        return if (duration == defaultVibrateDuration) {
            trigger.copy(vibrateDuration = null)
        } else {
            trigger.copy(vibrateDuration = duration)
        }
    }

    fun setLongPressDelay(trigger: Trigger, delay: Int, defaultLongPressDelay: Int): Trigger {
        return if (delay == defaultLongPressDelay) {
            trigger.copy(longPressDelay = null)
        } else {
            trigger.copy(longPressDelay = delay)
        }
    }

    fun setDoublePressDelay(trigger: Trigger, delay: Int, defaultDoublePressDelay: Int): Trigger {
        return if (delay == defaultDoublePressDelay) {
            trigger.copy(doublePressDelay = null)
        } else {
            trigger.copy(doublePressDelay = delay)
        }
    }

    fun setSequenceTriggerTimeout(
        trigger: Trigger,
        delay: Int,
        defaultSequenceTriggerTimeout: Int
    ): Trigger {
        return if (delay == defaultSequenceTriggerTimeout) {
            trigger.copy(sequenceTriggerTimeout = null)
        } else {
            trigger.copy(sequenceTriggerTimeout = delay)
        }
    }

    fun setLongPressDoubleVibrationEnabled(trigger: Trigger, enabled: Boolean): Trigger {
        return trigger.copy(longPressDoubleVibration = enabled)
    }

    fun setTriggerWhenScreenOff(trigger: Trigger, enabled: Boolean): Trigger {
        return trigger.copy(screenOffTrigger = enabled)
    }

    fun setTriggerFromOtherAppsEnabled(trigger: Trigger, enabled: Boolean): Trigger {
        return trigger.copy(triggerFromOtherApps = enabled)
    }

    fun setShowToastEnabled(trigger: Trigger, enabled: Boolean): Trigger {
        return trigger.copy(showToast = enabled)
    }

    fun setScanCodeDetectionEnabled(trigger: Trigger, keyUid: String, enabled: Boolean): Trigger {
        val newKeys = trigger.keys.map { key ->
            if (key.uid == keyUid && key is KeyCodeTriggerKey && key.isScanCodeDetectionUserConfigurable()) {
                when (key) {
                    is KeyEventTriggerKey -> {
                        key.copy(detectWithScanCodeUserSetting = enabled)
                    }

                    is EvdevTriggerKey -> {
                        key.copy(detectWithScanCodeUserSetting = enabled)
                    }
                }
            } else {
                key
            }
        }

        return trigger.copy(keys = newKeys)
    }
}