package io.github.sds100.keymapper.base.expertmode

import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupStep

data class ExpertModeSetupState(
    val stepNumber: Int,
    val stepCount: Int,
    val step: SystemBridgeSetupStep,
    val stepContent: StepContent,
    val isSetupAssistantChecked: Boolean,
    val isSetupAssistantButtonEnabled: Boolean,
    val startingMethod: SystemBridgeStartMethod?,
)
