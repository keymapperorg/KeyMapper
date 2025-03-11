package io.github.sds100.keymapper.mappings.keymaps

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.BaseMappingListItemCreator
import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.keymaps.trigger.AssistantTriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.AssistantTriggerType
import io.github.sds100.keymapper.mappings.keymaps.trigger.FloatingButtonKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyCodeTriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyEventDetectionSource
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.mappings.keymaps.trigger.Trigger
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerMode
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.flow.first

class KeyMapListItemCreator(
    private val displayMapping: DisplayKeyMapUseCase,
    resourceProvider: ResourceProvider,
) : BaseMappingListItemCreator<KeyMap, KeyMapAction>(
    displayMapping,
    KeyMapActionUiHelper(displayMapping, resourceProvider),
    resourceProvider,
) {
    private val midDot by lazy { getString(R.string.middot) }
    private val longPressString by lazy { getString(R.string.clicktype_long_press) }
    private val doublePressString by lazy { getString(R.string.clicktype_double_press) }
    private val anyAssistantString by lazy { getString(R.string.assistant_any_trigger_name) }
    private val voiceAssistantString by lazy { getString(R.string.assistant_voice_trigger_name) }
    private val deviceAssistantString by lazy { getString(R.string.assistant_device_trigger_name) }

    suspend fun create(
        keyMap: KeyMap,
        showDeviceDescriptors: Boolean,
    ): KeyMapListItemModel.Content {
        val triggerSeparator = when (keyMap.trigger.mode) {
            is TriggerMode.Parallel -> Icons.Outlined.Add
            else -> Icons.AutoMirrored.Outlined.ArrowForward
        }

        val triggerKeys = keyMap.trigger.keys.map { key ->
            when (key) {
                is AssistantTriggerKey -> assistantTriggerKeyName(key)
                is KeyCodeTriggerKey -> keyCodeTriggerKeyName(key, showDeviceDescriptors)
                is FloatingButtonKey -> floatingButtonKeyName(key)
            }
        }

        val optionsDescription = buildString {
            getTriggerOptionLabels(keyMap.trigger).forEachIndexed { index, label ->
                if (index != 0) {
                    append(" $midDot ")
                }

                append(label)
            }
        }

        val actionChipList = getActionChipList(keyMap, showDeviceDescriptors)
        val constraintChipList = getConstraintChipList(keyMap)

        val extraInfo = buildString {
            append(createExtraInfoString(keyMap, actionChipList, constraintChipList))

            if (keyMap.trigger.keys.isEmpty()) {
                if (this.isNotEmpty()) {
                    append(" $midDot ")
                }

                append(getString(R.string.no_trigger))
            }
        }

        val triggerErrorSnapshot = displayMapping.triggerErrorSnapshot.first()
        val triggerErrors = keyMap.trigger.keys.mapNotNull { key ->
            triggerErrorSnapshot.getTriggerError(
                keyMap,
                key,
            )
        }.distinct()

        return KeyMapListItemModel.Content(
            uid = keyMap.uid,
            triggerKeys = triggerKeys,
            triggerSeparatorIcon = triggerSeparator,
            triggerErrors = triggerErrors,
            actions = actionChipList,
            constraints = constraintChipList,
            constraintMode = keyMap.constraintState.mode,
            optionsDescription = optionsDescription.takeIf { it.isNotBlank() },
            extraInfo = extraInfo.takeIf { it.isNotBlank() },
        )
    }

    private fun floatingButtonKeyName(key: FloatingButtonKey): String = buildString {
        when (key.clickType) {
            ClickType.LONG_PRESS -> append(longPressString).append(" ")
            ClickType.DOUBLE_PRESS -> append(doublePressString).append(" ")
            else -> Unit
        }

        if (key.button == null) {
            append(getString(R.string.deleted_floating_button_text_key_map_list_item))
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

    private fun keyCodeTriggerKeyName(
        key: KeyCodeTriggerKey,
        showDeviceDescriptors: Boolean,
    ): String = buildString {
        when (key.clickType) {
            ClickType.LONG_PRESS -> append(longPressString).append(" ")
            ClickType.DOUBLE_PRESS -> append(doublePressString).append(" ")
            else -> Unit
        }

        append(InputEventUtils.keyCodeToString(key.keyCode))

        val deviceName = when (key.device) {
            is TriggerKeyDevice.Internal -> getString(R.string.this_device)
            is TriggerKeyDevice.Any -> getString(R.string.any_device)
            is TriggerKeyDevice.External -> {
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

        append(" (")

        if (key.detectionSource == KeyEventDetectionSource.INPUT_METHOD) {
            append("${getString(R.string.flag_detect_from_input_method)} $midDot ")
        }

        append(deviceName)

        if (!key.consumeEvent) {
            append(" $midDot ${getString(R.string.flag_dont_override_default_action)}")
        }

        append(")")
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

    private fun getTriggerOptionLabels(trigger: Trigger): List<String> {
        val labels = mutableListOf<String>()

        if (trigger.isVibrateAllowed() && trigger.vibrate) {
            labels.add(getString(R.string.flag_vibrate))
        }

        if (trigger.isLongPressDoubleVibrationAllowed() && trigger.longPressDoubleVibration) {
            labels.add(getString(R.string.flag_long_press_double_vibration))
        }

        if (trigger.isDetectingWhenScreenOffAllowed() && trigger.screenOffTrigger) {
            labels.add(getString(R.string.flag_detect_triggers_screen_off))
        }

        if (trigger.triggerFromOtherApps) {
            labels.add(getString(R.string.flag_trigger_from_other_apps))
        }

        if (trigger.showToast) {
            labels.add(getString(R.string.flag_show_toast))
        }

        return labels
    }
}
