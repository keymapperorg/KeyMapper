package io.github.sds100.keymapper.base.promode

import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupStep

data class ProModeSetupState(
    val stepNumber: Int,
    val stepCount: Int,
    val step: SystemBridgeSetupStep,
    val isSetupAssistantChecked: Boolean,
    val isSetupAssistantButtonEnabled: Boolean,
)
