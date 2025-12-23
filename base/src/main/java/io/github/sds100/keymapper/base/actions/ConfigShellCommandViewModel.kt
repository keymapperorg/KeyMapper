package io.github.sds100.keymapper.base.actions

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ExpertModeStatus
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.models.ShellExecutionMode
import io.github.sds100.keymapper.common.models.isExecuting
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.handle
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import java.util.Base64
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
    resourceProvider: ResourceProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider {

    var state: ShellCommandActionState by mutableStateOf(ShellCommandActionState())
        private set

    private var testJob: Job? = null

    init {
        // Update ExpertModeStatus in state
        if (Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API) {
            viewModelScope.launch {
                systemBridgeConnectionManager.connectionState.map { connectionState ->
                    when (connectionState) {
                        is SystemBridgeConnectionState.Connected -> ExpertModeStatus.ENABLED
                        is SystemBridgeConnectionState.Disconnected -> ExpertModeStatus.DISABLED
                    }
                }.collect { expertModeStatus ->
                    state = state.copy(expertModeStatus = expertModeStatus)
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
        state = state.copy(description = newDescription, descriptionError = null)
    }

    fun onCommandChanged(newCommand: String) {
        state = state.copy(command = newCommand, commandError = null)
        saveScriptText(newCommand)
    }

    fun onExecutionModeChanged(newExecutionMode: ShellExecutionMode) {
        state = state.copy(executionMode = newExecutionMode)
    }

    fun onTimeoutChanged(newTimeoutSeconds: Int) {
        state = state.copy(timeoutSeconds = newTimeoutSeconds)
    }

    fun onTestClick(): Boolean {
        testJob?.cancel()

        val commandError = validateCommand(state.command)
        if (commandError != null) {
            state = state.copy(commandError = commandError)
            return false
        }

        state = state.copy(
            isRunning = true,
            testResult = null,
        )

        testJob = viewModelScope.launch {
            testCommand()
        }

        return true
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

    fun onDoneClick(): Boolean {
        val commandError = validateCommand(state.command)
        if (commandError != null) {
            state = state.copy(commandError = commandError)
            return false
        }

        if (state.description.isBlank()) {
            state = state.copy(
                descriptionError = getString(R.string.error_cant_be_empty),
            )

            return false
        }

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

        return true
    }

    fun onCancelClick() {
        // Save script text before navigating away
        saveScriptText(state.command)

        viewModelScope.launch {
            navigationProvider.popBackStack()
        }
    }

    fun onSetupExpertModeClick() {
        viewModelScope.launch {
            navigationProvider.navigate(
                "shell_command_setup_expert_mode",
                NavDestination.ExpertModeSetup,
            )
        }
    }

    /**
     * @return the error message.
     */
    private fun validateCommand(command: String): String? {
        if (state.command.isBlank()) {
            return getString(R.string.action_shell_command_command_empty_error)
        }

        if (state.command.trimStart().startsWith("adb shell")) {
            return getString(R.string.action_shell_command_adb_shell_error)
        }

        return null
    }

    private fun saveScriptText(scriptText: String) {
        viewModelScope.launch {
            val encodedText = Base64.getEncoder().encodeToString(scriptText.toByteArray()).trim()
            preferenceRepository.set(Keys.shellCommandScriptText, encodedText)
        }
    }

    private fun loadScriptText() {
        viewModelScope.launch {
            preferenceRepository.get(Keys.shellCommandScriptText).collect { savedScriptText ->
                if (savedScriptText != null && state.command.isEmpty()) {
                    try {
                        val decodedText = String(Base64.getDecoder().decode(savedScriptText))
                        state = state.copy(command = decodedText)
                    } catch (e: Exception) {
                        // If decoding fails, ignore the saved text
                    }
                }
            }
        }
    }
}
