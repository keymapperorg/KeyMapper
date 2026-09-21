package io.github.sds100.keymapper.base.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.ActionErrorSnapshot
import io.github.sds100.keymapper.base.actions.ActionUiHelper
import io.github.sds100.keymapper.base.constraints.Constraint
import io.github.sds100.keymapper.base.constraints.ConstraintErrorSnapshot
import io.github.sds100.keymapper.base.constraints.ConstraintGroup
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.constraints.ConstraintState
import io.github.sds100.keymapper.base.constraints.ConstraintUiHelper
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.base.trigger.AssistantTriggerKey
import io.github.sds100.keymapper.base.trigger.AssistantTriggerType
import io.github.sds100.keymapper.base.trigger.EvdevTriggerKey
import io.github.sds100.keymapper.base.trigger.FingerprintTriggerKey
import io.github.sds100.keymapper.base.trigger.FloatingButtonKey
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerDevice
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerKey
import io.github.sds100.keymapper.base.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.base.trigger.Trigger
import io.github.sds100.keymapper.base.trigger.TriggerErrorSnapshot
import io.github.sds100.keymapper.base.trigger.TriggerKey
import io.github.sds100.keymapper.base.trigger.TriggerMode
import io.github.sds100.keymapper.base.trigger.getCodeLabel
import io.github.sds100.keymapper.base.utils.isFixable
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.common.utils.InputDeviceUtils
import io.github.sds100.keymapper.common.utils.KMError

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

                is KeyEventTriggerKey -> keyEventTriggerKeyName(
                    key,
                    keyMap.trigger.keys,
                    showDeviceDescriptors,
                )

                is FloatingButtonKey -> floatingButtonKeyName(key)

                is FingerprintTriggerKey -> fingerprintKeyName(key)

                is EvdevTriggerKey -> evdevTriggerKeyName(key)
            }
        }

        val options = getTriggerOptionLabels(keyMap.trigger)

        val actionChipList = getActionChipList(keyMap, showDeviceDescriptors, actionErrorSnapshot)
        val (constraintChipList, constraintMode) =
            buildConstraintChipList(
                keyMap.constraintState,
                constraintErrorSnapshot,
                isEnabled = keyMap.isEnabled,
            )

        val hasTriggerError =
            keyMap.trigger.keys.any { triggerErrorSnapshot.getTriggerError(keyMap, it) != null }

        val hasError: Boolean =
            hasTriggerError ||
                actionChipList.any { it is ComposeChipModel.Error } ||
                constraintChipList.any { it is ComposeChipModel.Error }

        return KeyMapListItemModel.Content(
            uid = keyMap.uid,
            triggerKeys = triggerKeys,
            triggerSeparatorIcon = triggerSeparator,
            actions = actionChipList,
            constraints = constraintChipList,
            constraintMode = constraintMode,
            options = options,
            isEnabled = keyMap.isEnabled,
            hasError = hasError,
        )
    }

    private val constraintUiHelper = ConstraintUiHelper(displayMapping, resourceProvider)

    private fun getActionChipList(
        keyMap: KeyMap,
        showDeviceDescriptors: Boolean,
        errorSnapshot: ActionErrorSnapshot,
    ): List<ComposeChipModel> = sequence {
        val midDot = getString(R.string.middot)

        val actionErrors = if (keyMap.isEnabled) {
            errorSnapshot.getErrors(keyMap.actionList.map { it.data })
        } else {
            emptyMap()
        }

        for (action in keyMap.actionList) {
            val actionTitle: String = if (action.multiplier != null) {
                "${action.multiplier}x ${actionUiHelper.getTitle(action, showDeviceDescriptors)}"
            } else {
                actionUiHelper.getTitle(action, showDeviceDescriptors)
            }

            val chipText = buildString {
                append(actionTitle)

                actionUiHelper.getOptionLabels(keyMap, action).forEach { label ->
                    append(" $midDot ")

                    append(label)
                }

                if (keyMap.isDelayBeforeNextActionAllowed() &&
                    action.delayBeforeNextAction != null
                ) {
                    if (this@buildString.isNotBlank()) {
                        append(" $midDot ")
                    }

                    append(
                        getString(
                            R.string.action_title_wait_ms,
                            action.delayBeforeNextAction,
                        ),
                    )
                }
            }

            val icon: ComposeIconInfo = actionUiHelper.getIcon(action.data)
            val error: KMError? = actionErrors[action.data]

            // Disabled actions or key maps are never performed so do not show their errors.
            val chip = if (error == null || !keyMap.isEnabled || !action.isEnabled) {
                ComposeChipModel.Normal(
                    id = action.uid,
                    text = chipText,
                    icon = icon,
                    isEnabled = keyMap.isEnabled && action.isEnabled,
                )
            } else {
                ComposeChipModel.Error(action.uid, chipText, error, isFixable = error.isFixable)
            }

            yield(chip)
        }
    }.toList()

    fun buildConstraintChipList(
        constraintState: ConstraintState,
        errorSnapshot: ConstraintErrorSnapshot,
        isEnabled: Boolean,
    ): Pair<List<ComposeChipModel>, ConstraintMode> {
        if (constraintState.groups.isEmpty()) {
            return Pair(emptyList(), constraintState.mode)
        }

        if (constraintState.groups.size == 1) {
            // If only one group then show the list of constraints as normal
            val firstGroup = constraintState.groups[0]
            val chips =
                firstGroup.constraints.map { buildConstraintChip(it, errorSnapshot, isEnabled) }

            return Pair(chips, firstGroup.mode)
        } else {
            val chips =
                constraintState.groups.mapNotNull {
                    buildGroupConstraintChip(
                        it,
                        errorSnapshot,
                        isEnabled,
                    )
                }

            return Pair(chips, constraintState.mode)
        }
    }

    private fun buildConstraintChip(
        constraint: Constraint,
        errorSnapshot: ConstraintErrorSnapshot,
        isEnabled: Boolean,
    ): ComposeChipModel {
        val text: String = constraintUiHelper.getTitle(constraint)
        val icon: ComposeIconInfo = constraintUiHelper.getIcon(constraint)
        val error: KMError? = errorSnapshot.getError(constraint)

        // Constraints for disabled key maps are never checked so do not show their errors.
        val chip: ComposeChipModel = if (error == null || !isEnabled) {
            ComposeChipModel.Normal(
                id = constraint.uid,
                text = text,
                icon = icon,
                isEnabled = isEnabled,
            )
        } else {
            ComposeChipModel.Error(
                constraint.uid,
                text,
                error,
                error.isFixable,
            )
        }
        return chip
    }

    private fun buildGroupConstraintChip(
        group: ConstraintGroup,
        errorSnapshot: ConstraintErrorSnapshot,
        isEnabled: Boolean,
    ): ComposeChipModel? {
        if (group.constraints.isEmpty()) {
            return null
        }

        if (group.constraints.size == 1) {
            return buildConstraintChip(group.constraints[0], errorSnapshot, isEnabled)
        } else {
            val text = if (group.name == null) {
                when (group.mode) {
                    ConstraintMode.AND -> getPluralString(
                        R.plurals.constraint_group_title_and,
                        group.constraints.size,
                        group.constraints.size,
                    )

                    ConstraintMode.OR -> getPluralString(
                        R.plurals.constraint_group_title_or,
                        group.constraints.size,
                        group.constraints.size,
                    )
                }
            } else {
                group.name
            }

            val icon: ComposeIconInfo? = null
            val error: KMError? =
                group.constraints.firstNotNullOfOrNull { errorSnapshot.getError(it) }

            // Constraints for disabled key maps are never checked so do not show their errors.
            val chip: ComposeChipModel = if (error == null || !isEnabled) {
                ComposeChipModel.Normal(
                    id = group.uid,
                    text = text,
                    icon = icon,
                    isEnabled = isEnabled,
                )
            } else {
                ComposeChipModel.Error(
                    id = group.uid,
                    text = text,
                    error = error,
                    isFixable = error.isFixable,
                )
            }
            return chip
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

        append(key.getCodeLabel(this@KeyMapListItemCreator))

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

        append(key.getCodeLabel(this@KeyMapListItemCreator))

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

    private fun getTriggerOptionLabels(trigger: Trigger): List<String> {
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
