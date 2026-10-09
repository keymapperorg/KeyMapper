package io.github.sds100.keymapper.base.expertmode

sealed class ExpertModeSetupState {
    abstract val stepNumber: Int
    abstract val stepCount: Int
    abstract val isSetupAssistantChecked: Boolean
    abstract val isSetupAssistantButtonEnabled: Boolean

    data class AccessibilityService(override val stepNumber: Int, override val stepCount: Int) :
        ExpertModeSetupState() {
        override val isSetupAssistantChecked: Boolean = false
        override val isSetupAssistantButtonEnabled: Boolean = false
    }

    data class NotificationPermission(
        override val stepNumber: Int,
        override val stepCount: Int,
        override val isSetupAssistantChecked: Boolean,
    ) : ExpertModeSetupState() {
        override val isSetupAssistantButtonEnabled: Boolean = true
    }

    data class LocalNetworkPermission(
        override val stepNumber: Int,
        override val stepCount: Int,
        override val isSetupAssistantChecked: Boolean,
    ) : ExpertModeSetupState() {
        override val isSetupAssistantButtonEnabled: Boolean = true
    }

    data class SamsungAutoBlocker(
        override val stepNumber: Int,
        override val stepCount: Int,
        override val isSetupAssistantChecked: Boolean,
    ) : ExpertModeSetupState() {
        override val isSetupAssistantButtonEnabled: Boolean = true
    }

    data class DeveloperOptions(
        override val stepNumber: Int,
        override val stepCount: Int,
        override val isSetupAssistantChecked: Boolean,
        val showRedactedWarning: Boolean,
    ) : ExpertModeSetupState() {
        override val isSetupAssistantButtonEnabled: Boolean = true
    }

    data class WifiNetwork(
        override val stepNumber: Int,
        override val stepCount: Int,
        override val isSetupAssistantChecked: Boolean,
    ) : ExpertModeSetupState() {
        override val isSetupAssistantButtonEnabled: Boolean = true
    }

    data class WirelessDebugging(
        override val stepNumber: Int,
        override val stepCount: Int,
        override val isSetupAssistantChecked: Boolean,
        val showSamsungAutoBlockerWarning: Boolean,
    ) : ExpertModeSetupState() {
        override val isSetupAssistantButtonEnabled: Boolean = true
    }

    data class AdbPairing(
        override val stepNumber: Int,
        override val stepCount: Int,
        override val isSetupAssistantChecked: Boolean,
    ) : ExpertModeSetupState() {
        override val isSetupAssistantButtonEnabled: Boolean = true
    }

    data class StartService(
        override val stepNumber: Int,
        override val stepCount: Int,
        override val isSetupAssistantChecked: Boolean,
        val isStarting: Boolean,
    ) : ExpertModeSetupState() {
        override val isSetupAssistantButtonEnabled: Boolean = true
    }

    data class Started(
        override val stepNumber: Int,
        override val stepCount: Int,
        override val isSetupAssistantChecked: Boolean,
    ) : ExpertModeSetupState() {
        override val isSetupAssistantButtonEnabled: Boolean = false
    }
}
