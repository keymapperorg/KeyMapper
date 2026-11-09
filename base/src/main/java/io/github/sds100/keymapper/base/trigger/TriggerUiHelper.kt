package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.InputDeviceUtils

class TriggerUiHelper(resourceProvider: ResourceProvider) : ResourceProvider by resourceProvider {
    private val midDot by lazy { getString(R.string.middot) }
    private val longPressString by lazy { getString(R.string.clicktype_long_press) }
    private val doublePressString by lazy { getString(R.string.clicktype_double_press) }
    private val anyAssistantString by lazy { getString(R.string.assistant_any_trigger_name) }
    private val voiceAssistantString by lazy { getString(R.string.assistant_voice_trigger_name) }
    private val deviceAssistantString by lazy { getString(R.string.assistant_device_trigger_name) }

    fun getKeyNames(trigger: Trigger, showDeviceDescriptors: Boolean): List<String> =
        trigger.keys.map { key ->
            when (key) {
                is AssistantTriggerKey -> assistantTriggerKeyName(key)

                is KeyEventTriggerKey -> keyEventTriggerKeyName(
                    key,
                    trigger.keys,
                    showDeviceDescriptors,
                )

                is FloatingButtonKey -> floatingButtonKeyName(key)

                is FingerprintTriggerKey -> fingerprintKeyName(key)

                is EvdevTriggerKey -> evdevTriggerKeyName(key)
            }
        }

    private fun floatingButtonKeyName(key: FloatingButtonKey): String = buildString {
        when (key.clickType) {
            ClickType.LONG_PRESS -> append(longPressString).append(" ")
            ClickType.DOUBLE_PRESS -> append(doublePressString).append(" ")
            else -> Unit
        }

        if (key.button == null) {
            append(getString(R.string.deleted_floating_button_text_key_map_list_item))
        } else if (key.button.appearance.text.isBlank()) {
            append(
                getString(
                    R.string.floating_button_text_key_map_list_item_empty,
                    key.button.layoutName,
                ),
            )
        } else {
            append(
                getString(
                    R.string.floating_button_text_key_map_list_item,
                    arrayOf(
                        key.button.appearance.text,
                        key.button.layoutName,
                    ),
                ),
            )
        }
    }

    private fun keyEventTriggerKeyName(
        key: KeyEventTriggerKey,
        allKeys: List<TriggerKey>,
        showDeviceDescriptors: Boolean,
    ): String = buildString {
        when (key.clickType) {
            ClickType.LONG_PRESS -> append(longPressString).append(" ")
            ClickType.DOUBLE_PRESS -> append(doublePressString).append(" ")
            else -> Unit
        }

        append(key.getCodeLabel(this@TriggerUiHelper))

        val deviceName = getTriggerKeyDeviceName(key, allKeys, showDeviceDescriptors)

        val parts = mutableListOf<String>()

        if (deviceName != null || key.requiresIme || !key.consumeEvent) {
            if (key.requiresIme) {
                parts.add(getString(R.string.flag_detect_from_input_method))
            }

            if (deviceName != null) {
                parts.add(deviceName)
            }

            if (!key.consumeEvent) {
                parts.add(getString(R.string.flag_dont_override_default_action))
            }
        }

        if (parts.isNotEmpty()) {
            append(" (")
            append(parts.joinToString(separator = " $midDot "))
            append(")")
        }
    }

    private fun getTriggerKeyDeviceName(
        key: KeyEventTriggerKey,
        allKeys: List<TriggerKey>,
        showDeviceDescriptors: Boolean,
    ): String? {
        val keyEventKeys = allKeys.filterIsInstance<KeyEventTriggerKey>()

        // If all the keys are Internal or Any, then do not show any extra text for brevity.
        // Adding the extra information for each key is extra verbosity without any significance.
        if (keyEventKeys.all { it.device == KeyEventTriggerDevice.Internal } ||
            keyEventKeys.all { it.device == KeyEventTriggerDevice.Any }
        ) {
            return null
        }

        return when (key.device) {
            is KeyEventTriggerDevice.Internal -> getString(R.string.this_device)

            is KeyEventTriggerDevice.Any -> getString(R.string.any_device)

            is KeyEventTriggerDevice.External -> {
                if (showDeviceDescriptors) {
                    InputDeviceUtils.appendDeviceDescriptorToName(
                        key.device.descriptor,
                        key.device.name,
                    )
                } else {
                    key.device.name
                }
            }
        }
    }

    private fun evdevTriggerKeyName(key: EvdevTriggerKey): String = buildString {
        when (key.clickType) {
            ClickType.LONG_PRESS -> append(longPressString).append(" ")
            ClickType.DOUBLE_PRESS -> append(doublePressString).append(" ")
            else -> Unit
        }

        append(key.getCodeLabel(this@TriggerUiHelper))

        val parts = buildList {
            add("Expert")
            add(key.device.name)

            if (!key.consumeEvent) {
                add(getString(R.string.flag_dont_override_default_action))
            }
        }

        if (parts.isNotEmpty()) {
            append(" (")
            append(parts.joinToString(separator = " $midDot "))
            append(")")
        }
    }

    private fun assistantTriggerKeyName(key: AssistantTriggerKey): String = buildString {
        when (key.clickType) {
            ClickType.DOUBLE_PRESS -> append(doublePressString).append(" ")
            else -> Unit
        }

        when (key.type) {
            AssistantTriggerType.ANY -> append(anyAssistantString)
            AssistantTriggerType.VOICE -> append(voiceAssistantString)
            AssistantTriggerType.DEVICE -> append(deviceAssistantString)
        }
    }

    private fun fingerprintKeyName(key: FingerprintTriggerKey): String = buildString {
        when (key.clickType) {
            ClickType.DOUBLE_PRESS -> append(doublePressString).append(" ")
            else -> Unit
        }

        when (key.type) {
            FingerprintGestureType.SWIPE_DOWN -> append(
                getString(R.string.trigger_key_fingerprint_gesture_down),
            )

            FingerprintGestureType.SWIPE_UP -> append(
                getString(R.string.trigger_key_fingerprint_gesture_up),
            )

            FingerprintGestureType.SWIPE_LEFT -> append(
                getString(R.string.trigger_key_fingerprint_gesture_left),
            )

            FingerprintGestureType.SWIPE_RIGHT -> append(
                getString(R.string.trigger_key_fingerprint_gesture_right),
            )
        }
    }

    fun getOptionLabels(trigger: Trigger): List<String> {
        val labels = mutableListOf<String>()

        if (trigger.isVibrateAllowed() && trigger.vibrate) {
            labels.add(getString(R.string.flag_vibrate))
        }

        if (trigger.isLongPressDoubleVibrationAllowed() && trigger.longPressDoubleVibration) {
            labels.add(getString(R.string.flag_long_press_double_vibration))
        }

        if (trigger.showToast) {
            labels.add(getString(R.string.flag_show_toast))
        }

        return labels
    }
}
