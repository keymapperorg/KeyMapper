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

    data class FingerprintGesture(
        val isAccessibilityServiceEnabled: Boolean,
        val areRequirementsMet: Boolean,
        val selectedType: FingerprintGestureType
    ) : TriggerSetupState()
}
