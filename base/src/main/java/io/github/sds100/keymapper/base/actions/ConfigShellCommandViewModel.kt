package io.github.sds100.keymapper.base.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.common.utils.KMError
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class ConfigShellCommandViewModel @Inject constructor(
    private val executeShellCommandUseCase: ExecuteShellCommandUseCase,
    private val navigationProvider: NavigationProvider,
) : ViewModel() {

    var state: ShellCommandActionState by mutableStateOf(ShellCommandActionState())
        private set

    private var testJob: Job? = null

    fun loadAction(action: ActionData.ShellCommand) {
        state = state.copy(
            description = action.description,
            command = action.command,
            useRoot = action.useRoot,
            timeoutSeconds = action.timeoutMs / 1000,
        )
    }

    fun onDescriptionChanged(newDescription: String) {
        state = state.copy(description = newDescription)
    }

    fun onCommandChanged(newCommand: String) {
        state = state.copy(command = newCommand)
    }

    fun onUseRootChanged(newUseRoot: Boolean) {
        state = state.copy(useRoot = newUseRoot)
    }

    fun onTimeoutChanged(newTimeoutSeconds: Int) {
        state = state.copy(timeoutSeconds = newTimeoutSeconds)
    }

    fun onTestClick() {
        // Cancel any existing test
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
                        useRoot = state.useRoot,
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
            useRoot = state.useRoot,
            timeoutMs = state.timeoutSeconds * 1000,
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
}
