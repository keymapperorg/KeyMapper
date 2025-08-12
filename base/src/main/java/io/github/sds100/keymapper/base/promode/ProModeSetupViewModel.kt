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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
        useCase.nextSetupStep.map { step ->
            State.Data(
                ProModeSetupState(
                    stepNumber = step.stepIndex + 1,
                    stepCount = SystemBridgeSetupStep.entries.size,
                    step = step,
                    isSetupAssistantEnabled = step == SystemBridgeSetupStep.ADB_PAIRING ||
                        step == SystemBridgeSetupStep.ADB_CONNECT ||
                        step == SystemBridgeSetupStep.START_SERVICE
                )
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            State.Loading
        )

    fun onButtonClick() {
        viewModelScope.launch {
            val currentStep = useCase.nextSetupStep.first()

            when (currentStep) {
                SystemBridgeSetupStep.ACCESSIBILITY_SERVICE -> useCase.enableAccessibilityService()
                SystemBridgeSetupStep.DEVELOPER_OPTIONS -> useCase.openDeveloperOptions()
                SystemBridgeSetupStep.WIFI_NETWORK -> useCase.connectWifiNetwork()
                SystemBridgeSetupStep.WIRELESS_DEBUGGING -> useCase.enableWirelessDebugging()
                SystemBridgeSetupStep.ADB_PAIRING, SystemBridgeSetupStep.ADB_CONNECT -> useCase.pairAdb()
                SystemBridgeSetupStep.START_SERVICE -> useCase.startSystemBridgeWithAdb()
            }
        }
    }

    fun onAssistantClick() {
        // TODO: Implement setup assistant logic
        // This could toggle a preference, show additional UI, or handle assistant-specific actions
    }
}

data class ProModeSetupState(
    val stepNumber: Int,
    val stepCount: Int,
    val step: SystemBridgeSetupStep,
    val isSetupAssistantEnabled: Boolean
)