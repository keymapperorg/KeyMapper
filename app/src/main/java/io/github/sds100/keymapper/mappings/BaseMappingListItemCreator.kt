package io.github.sds100.keymapper.mappings

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.Action
import io.github.sds100.keymapper.actions.ActionUiHelper
import io.github.sds100.keymapper.constraints.ConstraintMode
import io.github.sds100.keymapper.constraints.ConstraintUiHelper
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.ui.ChipUi
import io.github.sds100.keymapper.util.ui.IconInfo
import io.github.sds100.keymapper.util.ui.ResourceProvider

/**
 * Created by sds100 on 18/03/2021.
 */
abstract class BaseMappingListItemCreator<M : Mapping<A>, A : Action>(
    private val displayMapping: DisplaySimpleMappingUseCase,
    private val actionUiHelper: ActionUiHelper<M, A>,
    private val resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider {

    private val constraintUiHelper = ConstraintUiHelper(displayMapping, resourceProvider)

    fun getActionChipList(mapping: M, showDeviceDescriptors: Boolean): List<ChipUi> = sequence {
        val midDot = getString(R.string.middot)

        mapping.actionList.forEach { action ->
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

                actionUiHelper.getOptionLabels(mapping, action).forEach { label ->
                    append(" $midDot ")

                    append(label)
                }

                if (mapping.isDelayBeforeNextActionAllowed() && action.delayBeforeNextAction != null) {
                    if (this@buildString.isNotBlank()) {
                        append(" $midDot ")
                    }

                    append(
                        getString(
                            R.string.action_title_wait,
                            action.delayBeforeNextAction!!,
                        ),
                    )
                }
            }

            val icon: IconInfo? = actionUiHelper.getIcon(action.data)

            val error: Error? = displayMapping.getError(action.data)

            if (error == null) {
                val chip = ChipUi.Normal(id = action.uid, text = chipText, icon = icon)
                yield(chip)
            } else {
                val chip = ChipUi.Error(action.uid, chipText, error)

                yield(chip)
            }
        }
    }.toList()

    fun getConstraintChipList(mapping: M): List<ChipUi> = sequence {
        val constraintSeparatorText = when (mapping.constraintState.mode) {
            ConstraintMode.AND -> getString(R.string.constraint_mode_and)
            ConstraintMode.OR -> getString(R.string.constraint_mode_or)
        }

        mapping.constraintState.constraints.forEachIndexed { index, constraint ->
            if (index != 0) {
                yield(
                    ChipUi.Transparent(
                        id = constraintSeparatorText,
                        text = constraintSeparatorText,
                    ),
                )
            }

            val text: String = constraintUiHelper.getTitle(constraint)
            val icon: IconInfo? = constraintUiHelper.getIcon(constraint)
            val error: Error? = displayMapping.getConstraintError(constraint)

            val chip: ChipUi = if (error == null) {
                ChipUi.Normal(
                    id = constraint.uid,
                    text = text,
                    icon = icon,
                )
            } else {
                ChipUi.Error(
                    constraint.uid,
                    text,
                    error,
                )
            }

            yield(chip)
        }
    }.toList()

    fun createExtraInfoString(
        mapping: M,
        actionChipList: List<ChipUi>,
        constraintChipList: List<ChipUi>,
    ) = buildString {
        val midDot by lazy { getString(R.string.middot) }

        if (!mapping.isEnabled) {
            append(getString(R.string.disabled))
        }

        if (actionChipList.any { it is ChipUi.Error }) {
            if (this.isNotEmpty()) {
                append(" $midDot ")
            }

            append(getString(R.string.tap_actions_to_fix))
        }

        if (constraintChipList.any { it is ChipUi.Error }) {
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
}
