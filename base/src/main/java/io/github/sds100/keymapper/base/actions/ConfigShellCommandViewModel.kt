package io.github.sds100.keymapper.base.actions

import android.os.Build
import android.util.Base64
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
import io.github.sds100.keymapper.common.models.isExecuting
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.handle
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@HiltViewModel
class ConfigShellCommandViewModel @Inject constructor(
    private val executeShellCommandUseCase: ExecuteShellCommandUseCase,
    private val navigationProvider: NavigationProvider,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    var state: ShellCommandActionState by mutableStateOf(ShellCommandActionState())
        private set

    private var testJob: Job? = null

    init {
        // Update ProModeStatus in state
        if (Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API) {
            viewModelScope.launch {
                systemBridgeConnectionManager.connectionState.map { connectionState ->
                    when (connectionState) {
                        is SystemBridgeConnectionState.Connected -> ProModeStatus.ENABLED
                        is SystemBridgeConnectionState.Disconnected -> ProModeStatus.DISABLED
                    }
                }.collect { proModeStatus ->
                    state = state.copy(proModeStatus = proModeStatus)
                }
            }
        }

        // Load saved script text
        loadScriptText()
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
        saveScriptText(newCommand)
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
            testCommand()
        }
    }

    private suspend fun testCommand() {
        executeShellCommandUseCase.executeWithStreamingOutput(
            command = state.command,
            executionMode = state.executionMode,
            timeoutMillis = state.timeoutSeconds * 1000L,
        ).collect { result ->
            val isRunning = result.handle(
                onSuccess = { it.isExecuting() },
                onError = { false },
            )

            state = state.copy(isRunning = isRunning, testResult = result)
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

        // Save script text before navigating away
        saveScriptText(state.command)

        viewModelScope.launch {
            navigationProvider.popBackStackWithResult(Json.encodeToString(action))
        }
    }

    fun onCancelClick() {
        // Save script text before navigating away
        saveScriptText(state.command)

        viewModelScope.launch {
            navigationProvider.popBackStack()
        }
    }

    fun onSetupProModeClick() {
        viewModelScope.launch {
            navigationProvider.navigate("shell_command_setup_pro_mode", NavDestination.ProModeSetup)
        }
    }

    private fun saveScriptText(scriptText: String) {
        viewModelScope.launch {
            val encodedText = Base64.encodeToString(scriptText.toByteArray(), Base64.DEFAULT).trim()
            preferenceRepository.set(Keys.shellCommandScriptText, encodedText)
        }
    }

    private fun loadScriptText() {
        viewModelScope.launch {
            preferenceRepository.get(Keys.shellCommandScriptText).collect { savedScriptText ->
                if (savedScriptText != null && state.command.isEmpty()) {
                    try {
                        val decodedText = String(Base64.decode(savedScriptText, Base64.DEFAULT))
                        state = state.copy(command = decodedText)
                    } catch (e: Exception) {
                        // If decoding fails, ignore the saved text
                    }
                }
            }
        }
    }
}
