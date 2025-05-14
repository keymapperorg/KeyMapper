package io.github.sds100.keymapper.keymaps

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.ActionErrorSnapshot
import io.github.sds100.keymapper.actions.ActionUiHelper
import io.github.sds100.keymapper.constraints.ConstraintErrorSnapshot
import io.github.sds100.keymapper.constraints.ConstraintState
import io.github.sds100.keymapper.constraints.ConstraintUiHelper
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.trigger.AssistantTriggerKey
import io.github.sds100.keymapper.trigger.AssistantTriggerType
import io.github.sds100.keymapper.trigger.FingerprintTriggerKey
import io.github.sds100.keymapper.trigger.FloatingButtonKey
import io.github.sds100.keymapper.trigger.KeyEventDetectionSource
import io.github.sds100.keymapper.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.trigger.Trigger
import io.github.sds100.keymapper.trigger.TriggerErrorSnapshot
import io.github.sds100.keymapper.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.trigger.TriggerMode
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.isFixable
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo

class KeyMapListItemCreator(
    private val displayMapping: DisplayKeyMapUseCase,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider {
    private val midDot by lazy { getString(R.string.middot) }
    private val longPressString by lazy { getString(R.string.clicktype_long_press) }
    private val doublePressString by lazy { getString(R.string.clicktype_double_press) }
    private val anyAssistantString by lazy { getString(R.string.assistant_any_trigger_name) }
    private val voiceAssistantString by lazy { getString(R.string.assistant_voice_trigger_name) }
    private val deviceAssistantString by lazy { getString(R.string.assistant_device_trigger_name) }

    private val actionUiHelper = ActionUiHelper(displayMapping, resourceProvider)

    fun build(
        keyMap: KeyMap,
        showDeviceDescriptors: Boolean,
        triggerErrorSnapshot: TriggerErrorSnapshot,
        actionErrorSnapshot: ActionErrorSnapshot,
        constraintErrorSnapshot: ConstraintErrorSnapshot,
    ): KeyMapListItemModel.Content {
        val triggerSeparator = when (keyMap.trigger.mode) {
            is TriggerMode.Parallel -> Icons.Outlined.Add
            else -> Icons.AutoMirrored.Outlined.ArrowForward
        }

        val triggerKeys = keyMap.trigger.keys.map { key ->
            when (key) {
                is AssistantTriggerKey -> assistantTriggerKeyName(key)
                is io.github.sds100.keymapper.trigger.KeyCodeTriggerKey -> keyCodeTriggerKeyName(
                    key,
                    showDeviceDescriptors,
                )
                is FloatingButtonKey -> floatingButtonKeyName(key)
                is FingerprintTriggerKey -> fingerprintKeyName(key)
            }
        }

        val options = getTriggerOptionLabels(keyMap.trigger)

        val actionChipList = getActionChipList(keyMap, showDeviceDescriptors, actionErrorSnapshot)
        val constraintChipList =
            buildConstraintChipList(keyMap.constraintState, constraintErrorSnapshot)

        val extraInfo = buildString {
            append(createExtraInfoString(keyMap, actionChipList, constraintChipList))

            if (keyMap.trigger.keys.isEmpty()) {
                if (this.isNotEmpty()) {
                    append(" $midDot ")
                }

                append(getString(R.string.no_trigger))
            }
        }

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
            options = options,
            extraInfo = extraInfo.takeIf { it.isNotBlank() },
        )
    }

    private val constraintUiHelper = ConstraintUiHelper(displayMapping, resourceProvider)

    private fun getActionChipList(
        keyMap: KeyMap,
        showDeviceDescriptors: Boolean,
        errorSnapshot: ActionErrorSnapshot,
    ): List<ComposeChipModel> = sequence {
        val midDot = getString(R.string.middot)

        keyMap.actionList.forEach { action ->
            val actionTitle: String = if (action.multiplier != null) {
                "${action.multiplier}x ${
                    actionUiHelper.getTitle(
                        action.data,
                        showDeviceDescriptors,
                    )
                }"
            } else {
                actionUiHelper.getTitle(action.data, showDeviceDescriptors)
            }

            val chipText = buildString {
                append(actionTitle)

                actionUiHelper.getOptionLabels(keyMap, action).forEach { label ->
                    append(" $midDot ")

                    append(label)
                }

                if (keyMap.isDelayBeforeNextActionAllowed() && action.delayBeforeNextAction != null) {
                    if (this@buildString.isNotBlank()) {
                        append(" $midDot ")
                    }

                    append(
                        getString(
                            R.string.action_title_wait,
                            action.delayBeforeNextAction,
                        ),
                    )
                }
            }

            val icon: ComposeIconInfo = actionUiHelper.getIcon(action.data)
            val error: Error? = errorSnapshot.getError(action.data)

            val chip = if (error == null) {
                ComposeChipModel.Normal(id = action.uid, text = chipText, icon = icon)
            } else {
                ComposeChipModel.Error(action.uid, chipText, error, isFixable = error.isFixable)
            }

            yield(chip)
        }
    }.toList()

    fun buildConstraintChipList(
        constraintState: ConstraintState,
        errorSnapshot: ConstraintErrorSnapshot,
    ): List<ComposeChipModel> = sequence {
        for (constraint in constraintState.constraints) {
            val text: String = constraintUiHelper.getTitle(constraint)
            val icon: ComposeIconInfo = constraintUiHelper.getIcon(constraint)
            val error: Error? = errorSnapshot.getError(constraint)

            val chip: ComposeChipModel = if (error == null) {
                ComposeChipModel.Normal(
                    id = constraint.uid,
                    text = text,
                    icon = icon,
                )
            } else {
                ComposeChipModel.Error(constraint.uid, text, error, error.isFixable)
            }

            yield(chip)
        }
    }.toList()

    private fun createExtraInfoString(
        keyMap: KeyMap,
        actionChipList: List<ComposeChipModel>,
        constraintChipList: List<ComposeChipModel>,
    ) = buildString {
        val midDot by lazy { getString(R.string.middot) }

        if (!keyMap.isEnabled) {
            append(getString(R.string.switch_disabled))
        }

        if (actionChipList.any { it is ComposeChipModel.Error }) {
            if (this.isNotEmpty()) {
                append(" $midDot ")
            }

            append(getString(R.string.tap_actions_to_fix))
        }

        if (constraintChipList.any { it is ComposeChipModel.Error }) {
            if (this.isNotEmpty()) {
                append(" $midDot ")
            }

            append(getString(R.string.tap_constraints_to_fix))
        }

        if (actionChipList.isEmpty()) {
            if (this.isNotEmpty()) {
                append(" $midDot ")
            }

            append(getString(R.string.no_actions))
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
        key: io.github.sds100.keymapper.trigger.KeyCodeTriggerKey,
        showDeviceDescriptors: Boolean,
    ): String = buildString {
        when (key.clickType) {
            ClickType.LONG_PRESS -> append(longPressString).append(" ")
            ClickType.DOUBLE_PRESS -> append(doublePressString).append(" ")
            else -> Unit
        }

        append(InputEventUtils.keyCodeToString(key.keyCode))

        val deviceName = when (key.device) {
            is TriggerKeyDevice.Internal -> null
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

        val parts = mutableListOf<String>()

        if (deviceName != null || key.detectionSource == KeyEventDetectionSource.INPUT_METHOD || !key.consumeEvent) {
            if (key.detectionSource == KeyEventDetectionSource.INPUT_METHOD) {
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
            FingerprintGestureType.SWIPE_DOWN -> append(getString(R.string.trigger_key_fingerprint_gesture_down))
            FingerprintGestureType.SWIPE_UP -> append(getString(R.string.trigger_key_fingerprint_gesture_up))
            FingerprintGestureType.SWIPE_LEFT -> append(getString(R.string.trigger_key_fingerprint_gesture_left))
            FingerprintGestureType.SWIPE_RIGHT -> append(getString(R.string.trigger_key_fingerprint_gesture_right))
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
