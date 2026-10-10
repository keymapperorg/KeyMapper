package io.github.sds100.keymapper.base.expertmode

import android.os.Build
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

abstract class SystemBridgeSetupDelegateImpl(
    val viewModelScope: CoroutineScope,
    private val useCase: SystemBridgeSetupUseCase,
) : SystemBridgeSetupDelegate {
    override val setupState: StateFlow<State<ExpertModeSetupState>> =
        combine(
            useCase.nextSetupStep,
            useCase.isSetupAssistantEnabled,
            useCase.isSystemBridgeStarting,
            useCase.hasSamsungAutoBlocker,
            ::buildState,
        ).stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            State.Loading,
        )

    override fun onSetupStepButtonClick() {
        // Do not check the latest value in the use case because there is significant latency
        // when it is checking whether it is paired
        val currentState = setupState.value.dataOrNull() ?: return

        when (currentState) {
            is ExpertModeSetupState.AccessibilityService -> useCase.enableAccessibilityService()
            is ExpertModeSetupState.NotificationPermission ->
                useCase.requestNotificationPermission()
            is ExpertModeSetupState.LocalNetworkPermission ->
                useCase.requestLocalNetworkPermission()
            is ExpertModeSetupState.SamsungAutoBlocker -> useCase.openSamsungAutoBlockerSettings()
            is ExpertModeSetupState.DeveloperOptions -> useCase.enableDeveloperOptions()
            is ExpertModeSetupState.WifiNetwork -> useCase.connectWifiNetwork()
            is ExpertModeSetupState.WirelessDebugging -> useCase.enableWirelessDebugging()
            is ExpertModeSetupState.AdbPairing -> useCase.pairWirelessAdb()
            is ExpertModeSetupState.StartService -> useCase.startSystemBridgeWithAdb()
            is ExpertModeSetupState.Started -> onFinishClick()
        }
    }

    abstract fun onFinishClick()

    override fun onSetupAssistantClick() {
        useCase.toggleSetupAssistant()
    }

    override fun onSamsungAutoBlockerWarningClick() {
        useCase.openSamsungAutoBlockerSettings()
    }

    override fun onSkipDeveloperOptionsClick() {
        useCase.skipDeveloperOptionsStep()
    }

    private fun buildState(
        step: SystemBridgeSetupStep,
        isSetupAssistantUserEnabled: Boolean,
        isStarting: Boolean,
        hasSamsungAutoBlocker: Boolean,
    ): State.Data<ExpertModeSetupState> {
        val stepNumber = step.stepIndex + 1
        val stepCount = SystemBridgeSetupStep.entries.size

        val state = when (step) {
            SystemBridgeSetupStep.ACCESSIBILITY_SERVICE ->
                ExpertModeSetupState.AccessibilityService(
                    stepNumber = stepNumber,
                    stepCount = stepCount,
                )

            SystemBridgeSetupStep.NOTIFICATION_PERMISSION ->
                ExpertModeSetupState.NotificationPermission(
                    stepNumber = stepNumber,
                    stepCount = stepCount,
                    isSetupAssistantChecked = isSetupAssistantUserEnabled,
                )

            SystemBridgeSetupStep.ACCESS_LOCAL_NETWORK_PERMISSION ->
                ExpertModeSetupState.LocalNetworkPermission(
                    stepNumber = stepNumber,
                    stepCount = stepCount,
                    isSetupAssistantChecked = isSetupAssistantUserEnabled,
                )

            SystemBridgeSetupStep.SAMSUNG_AUTO_BLOCKER ->
                ExpertModeSetupState.SamsungAutoBlocker(
                    stepNumber = stepNumber,
                    stepCount = stepCount,
                    isSetupAssistantChecked = isSetupAssistantUserEnabled,
                )

            SystemBridgeSetupStep.DEVELOPER_OPTIONS ->
                ExpertModeSetupState.DeveloperOptions(
                    stepNumber = stepNumber,
                    stepCount = stepCount,
                    isSetupAssistantChecked = isSetupAssistantUserEnabled,
                    // See issue #2289. The developer options setting can be redacted on Android 17+.
                    showRedactedWarning =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN,
                )

            SystemBridgeSetupStep.WIFI_NETWORK ->
                ExpertModeSetupState.WifiNetwork(
                    stepNumber = stepNumber,
                    stepCount = stepCount,
                    isSetupAssistantChecked = isSetupAssistantUserEnabled,
                )

            SystemBridgeSetupStep.WIRELESS_DEBUGGING ->
                ExpertModeSetupState.WirelessDebugging(
                    stepNumber = stepNumber,
                    stepCount = stepCount,
                    isSetupAssistantChecked = isSetupAssistantUserEnabled,
                    showSamsungAutoBlockerWarning = hasSamsungAutoBlocker,
                )

            SystemBridgeSetupStep.ADB_PAIRING ->
                ExpertModeSetupState.AdbPairing(
                    stepNumber = stepNumber,
                    stepCount = stepCount,
                    isSetupAssistantChecked = isSetupAssistantUserEnabled,
                )

            SystemBridgeSetupStep.START_SERVICE ->
                ExpertModeSetupState.StartService(
                    stepNumber = stepNumber,
                    stepCount = stepCount,
                    isSetupAssistantChecked = isSetupAssistantUserEnabled,
                    isStarting = isStarting,
                )

            SystemBridgeSetupStep.STARTED ->
                ExpertModeSetupState.Started(
                    stepNumber = stepNumber,
                    stepCount = stepCount,
                    isSetupAssistantChecked = isSetupAssistantUserEnabled,
                )
        }

        return State.Data(state)
    }
}

interface SystemBridgeSetupDelegate {
    val setupState: StateFlow<State<ExpertModeSetupState>>
    fun onSetupStepButtonClick()
    fun onSetupAssistantClick()
    fun onSamsungAutoBlockerWarningClick()
    fun onSkipDeveloperOptionsClick()
}
