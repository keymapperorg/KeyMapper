package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType

sealed class TriggerSetupState {
    data class Volume(
        val isAccessibilityServiceEnabled: Boolean,
        val isScreenOffChecked: Boolean,
        val proModeStatus: ProModeStatus,
        val areRequirementsMet: Boolean,
        val recordTriggerState: RecordTriggerState,
    ) : TriggerSetupState()

    data class Keyboard(
        val isAccessibilityServiceEnabled: Boolean,
        val isScreenOffChecked: Boolean,
        val proModeStatus: ProModeStatus,
        val areRequirementsMet: Boolean,
        val recordTriggerState: RecordTriggerState,
    ) : TriggerSetupState()

    data class Power(
        val isAccessibilityServiceEnabled: Boolean,
        val proModeStatus: ProModeStatus,
        val areRequirementsMet: Boolean,
        val recordTriggerState: RecordTriggerState,
        val remapStatus: RemapStatus,
    ) : TriggerSetupState()

    data class Mouse(
        val isAccessibilityServiceEnabled: Boolean,
        val proModeStatus: ProModeStatus,
        val areRequirementsMet: Boolean,
        val recordTriggerState: RecordTriggerState,
        val remapStatus: RemapStatus,
    ) : TriggerSetupState()

    data class Other(
        val isAccessibilityServiceEnabled: Boolean,
        val isScreenOffChecked: Boolean,
        val proModeStatus: ProModeStatus,
        val areRequirementsMet: Boolean,
        val recordTriggerState: RecordTriggerState,
    ) : TriggerSetupState()

    sealed class Gamepad() : TriggerSetupState() {
        abstract val isAccessibilityServiceEnabled: Boolean
        abstract val areRequirementsMet: Boolean
        abstract val recordTriggerState: RecordTriggerState

        enum class Type {
            DPAD,
            SIMPLE_BUTTONS
        }

        data class Dpad(
            override val isAccessibilityServiceEnabled: Boolean,
            val isImeEnabled: Boolean,
            val isImeChosen: Boolean,
            override val areRequirementsMet: Boolean,
            override val recordTriggerState: RecordTriggerState,
        ) : Gamepad()

        data class SimpleButtons(
            override val isAccessibilityServiceEnabled: Boolean,
            val isScreenOffChecked: Boolean,
            val proModeStatus: ProModeStatus,
            override val areRequirementsMet: Boolean,
            override val recordTriggerState: RecordTriggerState,
        ) : Gamepad()
    }

    data class FingerprintGesture(
        val isAccessibilityServiceEnabled: Boolean,
        val areRequirementsMet: Boolean,
        val selectedType: FingerprintGestureType
    ) : TriggerSetupState()
}
