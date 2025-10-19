package io.github.sds100.keymapper.base.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.ProModeStatus
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.common.models.ShellExecutionMode
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class ConfigShellCommandViewModel @Inject constructor(
    private val executeShellCommandUseCase: ExecuteShellCommandUseCase,
    private val navigationProvider: NavigationProvider,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
) : ViewModel() {

    var state: ShellCommandActionState by mutableStateOf(ShellCommandActionState())
        private set

    private var testJob: Job? = null

    init {
        // Update ProModeStatus in state
        viewModelScope.launch {
            systemBridgeConnectionManager.connectionState.map { connectionState ->
                when (connectionState) {
                    is io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState.Connected -> ProModeStatus.ENABLED
                    is io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState.Disconnected -> ProModeStatus.DISABLED
                }
            }.collect { proModeStatus ->
                state = state.copy(proModeStatus = proModeStatus)
            }
        }
    }

    fun loadAction(action: ActionData.ShellCommand) {
        state = state.copy(
            description = action.description,
            command = action.command,
            executionMode = action.executionMode,
            timeoutSeconds = action.timeoutMillis / 1000,
        )
    }

    fun onDescriptionChanged(newDescription: String) {
        state = state.copy(description = newDescription)
    }

    fun onCommandChanged(newCommand: String) {
        state = state.copy(command = newCommand)
    }

    fun onExecutionModeChanged(newExecutionMode: ShellExecutionMode) {
        state = state.copy(executionMode = newExecutionMode)
    }

    fun onTimeoutChanged(newTimeoutSeconds: Int) {
        state = state.copy(timeoutSeconds = newTimeoutSeconds)
    }

    fun onTestClick() {
        testJob?.cancel()

        state = state.copy(
            isRunning = true,
            testResult = null,
        )

        testJob = viewModelScope.launch {
            try {
                withTimeout(state.timeoutSeconds * 1000L) {
                    val flow = executeShellCommandUseCase.executeWithStreamingOutput(
                        command = state.command,
                        executionMode = state.executionMode,
                    )

                    flow.catch { e ->
                        state = state.copy(
                            isRunning = false,
                            testResult = KMError.Exception(e as? Exception ?: Exception(e.message)),
                        )
                    }.collect { result ->
                        state = state.copy(
                            isRunning = false,
                            testResult = result,
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                state = state.copy(
                    isRunning = false,
                    testResult = KMError.ShellCommandTimeout(state.timeoutSeconds * 1000),
                )
            }
        }
    }

    fun onKillClick() {
        testJob?.cancel()
        state = state.copy(
            isRunning = false,
        )
    }

    fun onDoneClick() {
        val action = ActionData.ShellCommand(
            description = state.description,
            command = state.command,
            executionMode = state.executionMode,
            timeoutMillis = state.timeoutSeconds * 1000,
        )

        viewModelScope.launch {
            navigationProvider.popBackStackWithResult(Json.encodeToString(action))
        }
    }

    fun onCancelClick() {
        viewModelScope.launch {
            navigationProvider.popBackStack()
        }
    }

    fun onSetupProModeClick() {
        viewModelScope.launch {
            navigationProvider.navigate("shell_command_setup_pro_mode", NavDestination.ProModeSetup)
        }
    }
}
