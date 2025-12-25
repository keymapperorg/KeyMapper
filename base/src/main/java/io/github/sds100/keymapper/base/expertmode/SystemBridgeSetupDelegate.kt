package io.github.sds100.keymapper.base.expertmode

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.utils.ui.compose.icons.SignalWifiNotConnected
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
    private val resourceProvider: ResourceProvider,
) : SystemBridgeSetupDelegate,
    ResourceProvider by resourceProvider {
    override val setupState: StateFlow<State<ExpertModeSetupState>> =
        combine(
            useCase.nextSetupStep,
            useCase.isSetupAssistantEnabled,
            useCase.isSystemBridgeStarting,
            ::buildState,
        ).stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            State.Loading,
        )

    override fun onSetupStepButtonClick() {
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
            SystemBridgeSetupStep.START_SERVICE -> useCase.startSystemBridgeWithAdb()
            SystemBridgeSetupStep.STARTED -> onFinishClick()
        }
    }

    abstract fun onFinishClick()

    override fun onSetupAssistantClick() {
        useCase.toggleSetupAssistant()
    }

    override fun getStepContent(step: SystemBridgeSetupStep): StepContent {
        return when (step) {
            SystemBridgeSetupStep.ACCESSIBILITY_SERVICE -> StepContent(
                title = getString(
                    R.string.expert_mode_setup_wizard_enable_accessibility_service_title,
                ),
                message = getString(
                    R.string.expert_mode_setup_wizard_enable_accessibility_service_description,
                ),
                icon = Icons.Rounded.Accessibility,
                buttonText = getString(
                    R.string.expert_mode_setup_wizard_enable_accessibility_service_button,
                ),
            )

            SystemBridgeSetupStep.NOTIFICATION_PERMISSION -> StepContent(
                title = getString(
                    R.string.expert_mode_setup_wizard_enable_notification_permission_title,
                ),
                message = getString(
                    R.string.expert_mode_setup_wizard_enable_notification_permission_description,
                ),
                icon = Icons.Rounded.Notifications,
                buttonText = getString(
                    R.string.expert_mode_setup_wizard_enable_notification_permission_button,
                ),
            )

            SystemBridgeSetupStep.DEVELOPER_OPTIONS -> StepContent(
                title = getString(
                    R.string.expert_mode_setup_wizard_enable_developer_options_title,
                ),
                message = getString(
                    R.string.expert_mode_setup_wizard_enable_developer_options_description,
                ),
                icon = Icons.Rounded.Build,
                buttonText = getString(
                    R.string.expert_mode_setup_wizard_go_to_settings_button,
                ),
            )

            SystemBridgeSetupStep.WIFI_NETWORK -> StepContent(
                title = getString(
                    R.string.expert_mode_setup_wizard_connect_wifi_title,
                ),
                message = getString(
                    R.string.expert_mode_setup_wizard_connect_wifi_description,
                ),
                icon = KeyMapperIcons.SignalWifiNotConnected,
                buttonText = getString(
                    R.string.expert_mode_setup_wizard_go_to_settings_button,
                ),
            )

            SystemBridgeSetupStep.WIRELESS_DEBUGGING -> StepContent(
                title = getString(
                    R.string.expert_mode_setup_wizard_enable_wireless_debugging_title,
                ),
                message = getString(
                    R.string.expert_mode_setup_wizard_enable_wireless_debugging_description,
                ),
                icon = Icons.Rounded.BugReport,
                buttonText = getString(
                    R.string.expert_mode_setup_wizard_go_to_settings_button,
                ),
            )

            SystemBridgeSetupStep.ADB_PAIRING -> StepContent(
                title = getString(
                    R.string.expert_mode_setup_wizard_pair_wireless_debugging_title,
                ),
                message = getString(
                    R.string.expert_mode_setup_wizard_pair_wireless_debugging_description,
                ),
                icon = Icons.Rounded.Link,
                buttonText = getString(
                    R.string.expert_mode_setup_wizard_go_to_settings_button,
                ),
            )

            SystemBridgeSetupStep.START_SERVICE -> StepContent(
                title = getString(
                    R.string.expert_mode_setup_wizard_start_service_title,
                ),
                message = getString(
                    R.string.expert_mode_setup_wizard_start_service_description,
                ),
                icon = Icons.Rounded.PlayArrow,
                buttonText = getString(
                    R.string.expert_mode_root_detected_button_start_service,
                ),
            )

            SystemBridgeSetupStep.STARTED -> StepContent(
                title = getString(
                    R.string.expert_mode_setup_wizard_complete_title,
                ),
                message = getString(
                    R.string.expert_mode_setup_wizard_complete_text,
                ),
                icon = Icons.Rounded.CheckCircleOutline,
                buttonText = getString(
                    R.string.expert_mode_setup_wizard_complete_button,
                ),
            )
        }
    }

    private fun buildState(
        step: SystemBridgeSetupStep,
        isSetupAssistantUserEnabled: Boolean,
        isStarting: Boolean,
    ): State.Data<ExpertModeSetupState> {
        // Uncheck the setup assistant if the accessibility service is disabled since it is
        // required for the setup assistant to work
        val isSetupAssistantChecked = if (step == SystemBridgeSetupStep.ACCESSIBILITY_SERVICE) {
            false
        } else {
            isSetupAssistantUserEnabled
        }

        val stepContent = getStepContent(step)

        return State.Data(
            ExpertModeSetupState(
                stepNumber = step.stepIndex + 1,
                stepCount = SystemBridgeSetupStep.entries.size,
                step = step,
                stepContent = stepContent,
                isSetupAssistantChecked = isSetupAssistantChecked,
                isSetupAssistantButtonEnabled =
                step != SystemBridgeSetupStep.ACCESSIBILITY_SERVICE &&
                    step != SystemBridgeSetupStep.STARTED,
                isStarting = isStarting,
            ),
        )
    }
}

interface SystemBridgeSetupDelegate {
    val setupState: StateFlow<State<ExpertModeSetupState>>
    fun onSetupStepButtonClick()
    fun onSetupAssistantClick()
    fun getStepContent(step: SystemBridgeSetupStep): StepContent
}
