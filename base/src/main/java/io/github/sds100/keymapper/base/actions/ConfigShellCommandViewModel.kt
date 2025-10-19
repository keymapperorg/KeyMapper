package io.github.sds100.keymapper.base.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.popBackStackWithResult
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shell.ShellAdapter
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class ConfigShellCommandViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val shellAdapter: ShellAdapter,
    private val suAdapter: SuAdapter,
    private val navigationProvider: NavigationProvider,
) : ViewModel() {

    private val oldAction: ActionData.ShellCommand? =
        savedStateHandle.get<ActionData.ShellCommand?>("action")

    var command: String by mutableStateOf(oldAction?.command ?: "")
        private set

    var useRoot: Boolean by mutableStateOf(oldAction?.useRoot ?: false)
        private set

    var testResult: State<String>? by mutableStateOf(null)
        private set

    fun onCommandChanged(newCommand: String) {
        command = newCommand
    }

    fun onUseRootChanged(newUseRoot: Boolean) {
        useRoot = newUseRoot
    }

    fun onTestClick() {
        viewModelScope.launch {
            testResult = State.Loading

            val result = if (useRoot) {
                suAdapter.executeWithOutput(command)
            } else {
                shellAdapter.executeWithOutput(command)
            }

            testResult = when (result) {
                is Success -> State.Data(result.data)
                is KMError -> State.Data("Error: ${formatError(result)}")
            }
        }
    }

    fun onDoneClick() {
        val action = ActionData.ShellCommand(
            command = command,
            useRoot = useRoot,
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

    private fun formatError(error: KMError): String {
        return when (error) {
            is KMError.Exception -> error.exception.message ?: "Unknown error"
            else -> error.toString()
        }
    }
}
