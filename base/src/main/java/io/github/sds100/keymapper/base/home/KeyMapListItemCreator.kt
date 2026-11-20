package io.github.sds100.keymapper.base.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.rounded.Timelapse
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.ActionErrorSnapshot
import io.github.sds100.keymapper.base.actions.ActionUiHelper
import io.github.sds100.keymapper.base.constraints.Constraint
import io.github.sds100.keymapper.base.constraints.ConstraintErrorSnapshot
import io.github.sds100.keymapper.base.constraints.ConstraintGroup
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.constraints.ConstraintState
import io.github.sds100.keymapper.base.constraints.ConstraintUiHelper
import io.github.sds100.keymapper.base.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.base.trigger.TriggerErrorSnapshot
import io.github.sds100.keymapper.base.trigger.TriggerMode
import io.github.sds100.keymapper.base.trigger.TriggerUiHelper
import io.github.sds100.keymapper.base.utils.isFixable
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.TimeUtils

class KeyMapListItemCreator(
    private val displayMapping: DisplayKeyMapUseCase,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider {
    private val actionUiHelper = ActionUiHelper(displayMapping, resourceProvider)
    private val triggerUiHelper = TriggerUiHelper(resourceProvider)

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

        val triggerKeys = triggerUiHelper.getKeyNames(keyMap.trigger, showDeviceDescriptors)

        val options = triggerUiHelper.getOptionLabels(keyMap.trigger)

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
            name = keyMap.name,
        )
    }

    private val constraintUiHelper = ConstraintUiHelper(displayMapping, resourceProvider)

    private fun getActionChipList(
        keyMap: KeyMap,
        showDeviceDescriptors: Boolean,
        errorSnapshot: ActionErrorSnapshot,
    ): List<ComposeChipModel> = sequence {
        val actionErrors = if (keyMap.isEnabled) {
            errorSnapshot.getErrors(keyMap.actionList.map { it.data })
        } else {
            emptyMap()
        }

        for ((index, action) in keyMap.actionList.withIndex()) {
            val title = actionUiHelper.getChipTitle(keyMap, action, showDeviceDescriptors)
            val chipText = if (action.multiplier != null) {
                "${action.multiplier}x $title"
            } else {
                title
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

            // A delay after the last action does nothing so do not show it.
            if (keyMap.isDelayBeforeNextActionAllowed() &&
                action.delayBeforeNextAction != null &&
                index < keyMap.actionList.lastIndex
            ) {
                yield(
                    ComposeChipModel.Normal(
                        id = "${action.uid}-delay",
                        icon = ComposeIconInfo.Vector(Icons.Rounded.Timelapse),
                        text = delayText(action.delayBeforeNextAction),
                        isEnabled = keyMap.isEnabled && action.isEnabled,
                    ),
                )
            }
        }
    }.toList()

    private fun delayText(ms: Int): String = if (ms < 1000) {
        getString(R.string.action_chip_delay_ms, ms)
    } else {
        getString(R.string.action_chip_delay_secs, TimeUtils.formatSeconds(ms))
    }

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
}
