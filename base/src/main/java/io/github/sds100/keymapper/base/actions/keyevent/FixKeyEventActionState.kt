package io.github.sds100.keymapper.base.actions.keyevent

import io.github.sds100.keymapper.base.trigger.ProModeStatus

sealed class FixKeyEventActionState {
    abstract val isAccessibilityServiceEnabled: Boolean

    data class InputMethod(
        val isEnabled: Boolean,
        val isChosen: Boolean,
        /**
         * Accessibility services can enable the input method on Android 13+ so only
         * show one button that both enables and chooses the input method.
         */
        val enablingRequiresUserInput: Boolean,
        override val isAccessibilityServiceEnabled: Boolean
    ) : FixKeyEventActionState()

    data class ProMode(
        val proModeStatus: ProModeStatus,
        override val isAccessibilityServiceEnabled: Boolean
    ) : FixKeyEventActionState()
}
