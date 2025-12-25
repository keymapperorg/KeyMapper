package io.github.sds100.keymapper.base.actions.keyevent

import io.github.sds100.keymapper.base.utils.ExpertModeStatus

sealed class FixKeyEventActionState {
    abstract val isAccessibilityServiceEnabled: Boolean
    abstract val expertModeStatus: ExpertModeStatus

    data class InputMethod(
        val isEnabled: Boolean,
        val isChosen: Boolean,
        /**
         * Accessibility services can enable the input method on Android 13+ so only
         * show one button that both enables and chooses the input method.
         */
        val enablingRequiresUserInput: Boolean,
        val isAutoSwitchImeEnabled: Boolean,
        override val isAccessibilityServiceEnabled: Boolean,
        override val expertModeStatus: ExpertModeStatus,
    ) : FixKeyEventActionState()

    data class ExpertMode(
        override val isAccessibilityServiceEnabled: Boolean,
        override val expertModeStatus: ExpertModeStatus,
    ) : FixKeyEventActionState()
}
