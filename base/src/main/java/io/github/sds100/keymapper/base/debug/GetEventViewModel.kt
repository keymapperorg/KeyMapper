package io.github.sds100.keymapper.base.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.utils.ExpertModeStatus
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class GetEventViewModel @Inject constructor(
    private val outputUseCase: GetEventRecorder,
    private val navigationProvider: NavigationProvider,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    private val pauseKeyMapsUseCase: PauseKeyMapsUseCase,
) : ViewModel(),
    NavigationProvider by navigationProvider {

    private var resumeKeyMapsOnStop = false

    var state: GetEventState by mutableStateOf(GetEventState())
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
            outputUseCase.isRecording.collect { isRecording ->
                if (!isRecording && resumeKeyMapsOnStop) {
                    pauseKeyMapsUseCase.resume()
                    resumeKeyMapsOnStop = false
                }

                state = state.copy(isRecording = isRecording)
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
        // Only unpause key maps when recording stops if the user hadn't previously paused it
        // themselves.
        resumeKeyMapsOnStop = !pauseKeyMapsUseCase.isPaused.firstBlocking()

        // Key maps should be paused while recording because any events from grabbed devices
        // will not appear in the getevent log.
        pauseKeyMapsUseCase.pause()
        outputUseCase.recordEvents()
    }

    private fun stopRecording() {
        viewModelScope.launch {
            outputUseCase.stopRecording()
        }
    }

    fun onCopyToClipboardClick() {
        outputUseCase.copyOutput(getCombinedOutput())
    }

    fun onSaveToFileClick() {
        viewModelScope.launch {
            outputUseCase.shareOutput(getCombinedOutput())
        }
    }

    private fun getCombinedOutput(): String = buildString {
        if (state.deviceInfoOutput.isNotBlank()) {
            append(state.deviceInfoOutput)
        }

        if (state.recordingOutput.isNotBlank()) {
            if (isNotEmpty()) {
                append("\n\n")
            }
            append(state.recordingOutput)
        }
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

data class GetEventState(
    val deviceInfoOutput: String = "",
    val recordingOutput: String = "",
    val isLoadingDeviceInfo: Boolean = false,
    val isRecording: Boolean = false,
    val expertModeStatus: ExpertModeStatus = ExpertModeStatus.DISABLED,
)
