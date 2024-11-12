package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.BaseMappingListItemCreator
import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.keymaps.trigger.AssistantTriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.AssistantTriggerType
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyCodeTriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.Trigger
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerError
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerMode
import io.github.sds100.keymapper.purchasing.ProductId
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.ui.ChipUi
import io.github.sds100.keymapper.util.ui.ResourceProvider

/**
 * Created by sds100 on 19/03/2021.
 */
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
    ): KeyMapListItem.KeyMapUiState {
        val triggerDescription = buildString {
            val separator = when (keyMap.trigger.mode) {
                is TriggerMode.Parallel -> getString(R.string.plus)
                is TriggerMode.Sequence -> getString(R.string.arrow)
                is TriggerMode.Undefined -> null
            }

            keyMap.trigger.keys.forEachIndexed { index, key ->
                if (index > 0) {
                    append(" $separator ")
                }

                when (key) {
                    is AssistantTriggerKey -> appendAssistantTriggerKeyName(key)
                    is KeyCodeTriggerKey -> appendKeyCodeTriggerKeyName(key, showDeviceDescriptors)
                }
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

        val triggerErrors = displayMapping.getTriggerErrors(keyMap)

        val triggerErrorChips = triggerErrors.map(this::getTriggerChipError)

        return KeyMapListItem.KeyMapUiState(
            uid = keyMap.uid,
            actionChipList = actionChipList,
            constraintChipList = constraintChipList,
            triggerDescription = triggerDescription,
            optionsDescription = optionsDescription,
            extraInfo = extraInfo,
            triggerErrorChipList = triggerErrorChips,
        )
    }

    private fun getTriggerChipError(error: TriggerError): ChipUi.Error =
        when (error) {
            TriggerError.DND_ACCESS_DENIED ->
                ChipUi.Error(
                    id = TriggerError.DND_ACCESS_DENIED.toString(),
                    text = getString(R.string.trigger_error_dnd_access_denied_short),
                    error = Error.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY),
                )

            TriggerError.SCREEN_OFF_ROOT_DENIED -> ChipUi.Error(
                id = TriggerError.SCREEN_OFF_ROOT_DENIED.toString(),
                text = getString(R.string.trigger_error_screen_off_root_permission_denied_short),
                error = Error.PermissionDenied(Permission.ROOT),
            )

            TriggerError.CANT_DETECT_IN_PHONE_CALL -> ChipUi.Error(
                id = TriggerError.SCREEN_OFF_ROOT_DENIED.toString(),
                text = getString(R.string.trigger_error_cant_detect_in_phone_call),
                error = Error.CantDetectKeyEventsInPhoneCall,
            )

            TriggerError.ASSISTANT_NOT_SELECTED -> ChipUi.Error(
                id = error.toString(),
                text = getString(R.string.trigger_error_assistant_activity_not_chosen),
                error = Error.DeviceAssistantNotSet,
            )

            TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED -> ChipUi.Error(
                id = error.toString(),
                text = getString(R.string.trigger_error_assistant_not_purchased),
                error = Error.ProductNotPurchased(ProductId.ASSISTANT_TRIGGER),
            )
        }

    private fun StringBuilder.appendKeyCodeTriggerKeyName(
        key: KeyCodeTriggerKey,
        showDeviceDescriptors: Boolean,
    ) {
        when (key.clickType) {
            ClickType.LONG_PRESS -> append(longPressString).append(" ")
            ClickType.DOUBLE_PRESS -> append(doublePressString).append(" ")
            else -> Unit
        }

        append(KeyEventUtils.keyCodeToString(key.keyCode))

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

        append(deviceName)

        if (!key.consumeEvent) {
            append(" $midDot ${getString(R.string.flag_dont_override_default_action)}")
        }

        append(")")
    }

    private fun StringBuilder.appendAssistantTriggerKeyName(key: AssistantTriggerKey) {
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
