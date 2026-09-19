package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo

data class ActionListItemModel(
    val id: String,
    val icon: ComposeIconInfo,
    val title: String,
    val isExpanded: Boolean = false,
    val isCustomName: Boolean = false,
    val isEnabled: Boolean = true,
    /**
     * A summary of the options that is shown when the item is collapsed.
     */
    val summary: String? = null,
    val error: String? = null,
    val isErrorFixable: Boolean = true,
    val showRepeat: Boolean = false,
    /**
     * Null if the action does not repeat.
     */
    val repeatText: String? = null,
    /**
     * Null if the action is not performed in a burst.
     */
    val burstText: String? = null,
    val showHoldDown: Boolean = false,
    /**
     * Null if the action is not held down.
     */
    val holdDownText: String? = null,
    /**
     * Whether to show the chip for setting the delay before the next action. A delay after the
     * last action does nothing.
     */
    val showDelayChip: Boolean = false,
    val delayBeforeNextAction: Int? = null,
)
