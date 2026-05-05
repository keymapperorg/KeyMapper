package io.github.sds100.keymapper.base.debug

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.base.actions.ExecuteShellCommandUseCase
import io.github.sds100.keymapper.base.utils.ExpertModeStatus
import io.github.sds100.keymapper.base.utils.ShareUtils
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.models.ShellExecutionMode
import io.github.sds100.keymapper.common.utils.handle
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.system.clipboard.ClipboardAdapter
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.FileUtils
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class GetEventViewModel @Inject constructor(
    private val executeShellCommandUseCase: ExecuteShellCommandUseCase,
    private val navigationProvider: NavigationProvider,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    private val clipboardAdapter: ClipboardAdapter,
    private val fileAdapter: FileAdapter,
    private val buildConfigProvider: BuildConfigProvider,
    @ApplicationContext private val context: Context,
    resourceProvider: ResourceProvider,
) : ViewModel(),
    NavigationProvider by navigationProvider,
    ResourceProvider by resourceProvider {

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
                    loadDeviceInfo()
                }
            }
        }
    }

    private fun loadDeviceInfo() {
        viewModelScope.launch {
            state = state.copy(isLoadingDeviceInfo = true)
            val result = executeShellCommandUseCase.execute(
                command = "getevent -il",
                executionMode = ShellExecutionMode.ADB,
                timeoutMillis = 30_000L,
            )
            val output = result.handle(
                onSuccess = { it.stdout },
                onError = { "Error: ${it.getFullMessage(this@GetEventViewModel)}" },
            )
            state = state.copy(deviceInfoOutput = output, isLoadingDeviceInfo = false)
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
        state = state.copy(isRecording = true)
        viewModelScope.launch {
            val result = executeShellCommandUseCase.execute(
                command = "getevent -lt",
                executionMode = ShellExecutionMode.ADB,
                timeoutMillis = 300_000L,
            )
            val newOutput = result.handle(
                onSuccess = { it.stdout },
                onError = { "" },
            )
            if (newOutput.isNotEmpty()) {
                state = state.copy(
                    recordingOutput = state.recordingOutput + newOutput,
                )
            }
            state = state.copy(isRecording = false)
        }
    }

    private fun stopRecording() {
        viewModelScope.launch {
            executeShellCommandUseCase.execute(
                command = "pkill -x getevent || true",
                executionMode = ShellExecutionMode.ADB,
                timeoutMillis = 5_000L,
            )
        }
    }

    fun onClearClick() {
        state = state.copy(recordingOutput = "")
    }

    fun onCopyToClipboardClick() {
        clipboardAdapter.copy("getevent output", state.recordingOutput)
    }

    fun onSaveToFileClick() {
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = "getevent/key_mapper_getevent_${FileUtils.createFileDate()}.txt"
            val file = fileAdapter.getPrivateFile(fileName)
            file.createFile()
            file.outputStream()?.bufferedWriter()?.use { it.write(state.recordingOutput) }
            val publicUri = fileAdapter.getPublicUriForPrivateFile(file).toUri()
            ShareUtils.shareFile(context, publicUri, buildConfigProvider.packageName)
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
