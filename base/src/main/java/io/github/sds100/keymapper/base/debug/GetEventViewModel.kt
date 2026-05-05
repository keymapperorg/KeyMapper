package io.github.sds100.keymapper.base.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.ExpertModeStatus
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class GetEventViewModel @Inject constructor(
    private val outputUseCase: GetEventOutputUseCase,
    private val navigationProvider: NavigationProvider,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
) : ViewModel(),
    NavigationProvider by navigationProvider {

    enum class OutputTab {
        INFO,
        EVENTS,
    }

    data class State(
        val deviceInfoOutput: String = "",
        val recordingOutput: String = "",
        val isLoadingDeviceInfo: Boolean = false,
        val isRecording: Boolean = false,
        val expertModeStatus: ExpertModeStatus = ExpertModeStatus.DISABLED,
    )

    var state: State by mutableStateOf(State())
        private set

    init {
        viewModelScope.launch {
            outputUseCase.deviceInfoOutput.collect { output ->
                state = state.copy(deviceInfoOutput = output)
            }
        }

        viewModelScope.launch {
            outputUseCase.eventsOutput.collect { output ->
                state = state.copy(recordingOutput = output)
            }
        }

        viewModelScope.launch {
            systemBridgeConnectionManager.connectionState.map { connectionState ->
                when (connectionState) {
                    is SystemBridgeConnectionState.Connected -> ExpertModeStatus.ENABLED
                    is SystemBridgeConnectionState.Disconnected -> ExpertModeStatus.DISABLED
                }
            }.collect { status ->
                val wasDisabled = state.expertModeStatus != ExpertModeStatus.ENABLED
                state = state.copy(expertModeStatus = status)
                if (status == ExpertModeStatus.ENABLED &&
                    wasDisabled &&
                    state.deviceInfoOutput.isEmpty()
                ) {
                    onRefreshDeviceInfoClick()
                }
            }
        }
    }

    fun onRefreshDeviceInfoClick() {
        viewModelScope.launch {
            state = state.copy(isLoadingDeviceInfo = true)
            outputUseCase.refreshDeviceInfo()
            state = state.copy(isLoadingDeviceInfo = false)
        }
    }

    fun onToggleRecordClick() {
        if (state.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        viewModelScope.launch {
            state = state.copy(isRecording = true)
            outputUseCase.recordEvents()
            state = state.copy(isRecording = false)
        }
    }

    private fun stopRecording() {
        viewModelScope.launch {
            outputUseCase.stopRecording()
        }
    }

    fun onCopyToClipboardClick(tab: OutputTab) {
        outputUseCase.copyOutput(getOutputForTab(tab))
    }

    fun onSaveToFileClick(tab: OutputTab) {
        viewModelScope.launch {
            outputUseCase.shareOutput(getOutputForTab(tab))
        }
    }

    private fun getOutputForTab(tab: OutputTab): String = when (tab) {
        OutputTab.INFO -> state.deviceInfoOutput
        OutputTab.EVENTS -> state.recordingOutput
    }

    fun onBackClick() {
        viewModelScope.launch {
            navigationProvider.popBackStack()
        }
    }

    fun onSetupExpertModeClick() {
        viewModelScope.launch {
            navigationProvider.navigate(
                "getevent_setup_expert_mode",
                NavDestination.ExpertModeSetup,
            )
        }
    }
}
