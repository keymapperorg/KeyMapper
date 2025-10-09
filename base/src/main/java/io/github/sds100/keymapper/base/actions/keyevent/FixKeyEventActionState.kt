package io.github.sds100.keymapper.base.actions.keyevent

import io.github.sds100.keymapper.base.trigger.ProModeStatus

sealed class FixKeyEventActionState {
    abstract val isAccessibilityServiceEnabled: Boolean
    abstract val proModeStatus: ProModeStatus

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
        override val proModeStatus: ProModeStatus,
    ) : FixKeyEventActionState()

    data class ProMode(
        override val isAccessibilityServiceEnabled: Boolean,
        override val proModeStatus: ProModeStatus,
    ) : FixKeyEventActionState()
}
