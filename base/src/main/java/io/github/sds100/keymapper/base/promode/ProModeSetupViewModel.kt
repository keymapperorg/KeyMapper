package io.github.sds100.keymapper.base.promode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupStep
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProModeSetupViewModel @Inject constructor(
    private val useCase: SystemBridgeSetupUseCase,
    navigationProvider: NavigationProvider,
    resourceProvider: ResourceProvider
) : ViewModel(), NavigationProvider by navigationProvider, ResourceProvider by resourceProvider {
    val setupState: StateFlow<State<ProModeSetupState>> =
        combine(useCase.nextSetupStep, useCase.isSetupAssistantEnabled, ::buildState).stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            State.Loading
        )

    fun onStepButtonClick() {
        viewModelScope.launch {
            val currentStep = useCase.nextSetupStep.first()

            when (currentStep) {
                SystemBridgeSetupStep.ACCESSIBILITY_SERVICE -> useCase.enableAccessibilityService()
                SystemBridgeSetupStep.NOTIFICATION_PERMISSION -> useCase.requestNotificationPermission()
                SystemBridgeSetupStep.DEVELOPER_OPTIONS -> useCase.openDeveloperOptions()
                SystemBridgeSetupStep.WIFI_NETWORK -> useCase.connectWifiNetwork()
                SystemBridgeSetupStep.WIRELESS_DEBUGGING -> useCase.enableWirelessDebugging()
                SystemBridgeSetupStep.ADB_PAIRING -> useCase.pairWirelessAdb()
                SystemBridgeSetupStep.START_SERVICE -> useCase.startSystemBridgeWithAdb()
                SystemBridgeSetupStep.STARTED -> popBackStack()
            }
        }
    }

    private fun buildState(
        step: SystemBridgeSetupStep,
        isSetupAssistantEnabled: Boolean
    ): State.Data<ProModeSetupState> = State.Data(
        ProModeSetupState(
            stepNumber = step.stepIndex + 1,
            stepCount = SystemBridgeSetupStep.entries.size,
            step = step,
            isSetupAssistantChecked = isSetupAssistantEnabled
        )
    )

    fun onAssistantClick() {
        useCase.toggleSetupAssistant()
    }
}

data class ProModeSetupState(
    val stepNumber: Int,
    val stepCount: Int,
    val step: SystemBridgeSetupStep,
    val isSetupAssistantChecked: Boolean
)