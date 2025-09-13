package io.github.sds100.keymapper.base.trigger

sealed class TriggerSetupState {
    data class Volume(
        val isAccessibilityServiceEnabled: Boolean,
        val isScreenOffChecked: Boolean,
        val proModeStatus: ProModeStatus,
        val areRequirementsMet: Boolean,
        val recordTriggerState: RecordTriggerState,
        val remapStatus: RemapStatus,
    ) : TriggerSetupState()

    data class Power(
        val isAccessibilityServiceEnabled: Boolean,
        val proModeStatus: ProModeStatus,
        val areRequirementsMet: Boolean,
        val recordTriggerState: RecordTriggerState,
        val remapStatus: RemapStatus,
    ) : TriggerSetupState()
}
