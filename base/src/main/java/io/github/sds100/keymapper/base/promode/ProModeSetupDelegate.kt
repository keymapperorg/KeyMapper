package io.github.sds100.keymapper.base.promode

import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupStep
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@ViewModelScoped
class ProModeSetupDelegateImpl @Inject constructor(
    @Named("viewmodel")
    val viewModelScope: CoroutineScope,
    private val useCase: SystemBridgeSetupUseCase,
) : ProModeSetupDelegate {
    override val setupState: StateFlow<State<ProModeSetupState>> =
        combine(useCase.nextSetupStep, useCase.isSetupAssistantEnabled, ::buildState).stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            State.Loading,
        )

    override fun onStepButtonClick() {
        // Do not check the latest value in the use case because there is significant latency
        // when it is checking whether it is paired
        val currentStep = setupState.value.dataOrNull()?.step ?: return

        when (currentStep) {
            SystemBridgeSetupStep.ACCESSIBILITY_SERVICE -> useCase.enableAccessibilityService()
            SystemBridgeSetupStep.NOTIFICATION_PERMISSION -> useCase.requestNotificationPermission()
            SystemBridgeSetupStep.DEVELOPER_OPTIONS -> useCase.enableDeveloperOptions()
            SystemBridgeSetupStep.WIFI_NETWORK -> useCase.connectWifiNetwork()
            SystemBridgeSetupStep.WIRELESS_DEBUGGING -> useCase.enableWirelessDebugging()
            SystemBridgeSetupStep.ADB_PAIRING -> useCase.pairWirelessAdb()
            SystemBridgeSetupStep.START_SERVICE -> viewModelScope.launch {
                useCase.startSystemBridgeWithAdb()
            }
            SystemBridgeSetupStep.STARTED -> {
                // TODO
            }
        }
    }

    override fun onAssistantClick() {
        useCase.toggleSetupAssistant()
    }

    private fun buildState(
        step: SystemBridgeSetupStep,
        isSetupAssistantUserEnabled: Boolean,
    ): State.Data<ProModeSetupState> {
        // Uncheck the setup assistant if the accessibility service is disabled since it is
        // required for the setup assistant to work
        val isSetupAssistantChecked = if (step == SystemBridgeSetupStep.ACCESSIBILITY_SERVICE) {
            false
        } else {
            isSetupAssistantUserEnabled
        }

        return State.Data(
            ProModeSetupState(
                stepNumber = step.stepIndex + 1,
                stepCount = SystemBridgeSetupStep.entries.size,
                step = step,
                isSetupAssistantChecked = isSetupAssistantChecked,
                isSetupAssistantButtonEnabled =
                step != SystemBridgeSetupStep.ACCESSIBILITY_SERVICE &&
                    step != SystemBridgeSetupStep.STARTED,
            ),
        )
    }
}

interface ProModeSetupDelegate {
    val setupState: StateFlow<State<ProModeSetupState>>
    fun onStepButtonClick()
    fun onAssistantClick()
}
