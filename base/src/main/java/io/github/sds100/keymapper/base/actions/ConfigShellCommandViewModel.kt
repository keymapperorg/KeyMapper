package io.github.sds100.keymapper.base.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.popBackStackWithResult
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
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
    private val resourceProvider: ResourceProvider,
) : ViewModel() {

    private val oldAction: ActionData.ShellCommand? =
        savedStateHandle.get<ActionData.ShellCommand?>("action")

    var state: ShellCommandActionState by mutableStateOf(
        ShellCommandActionState(
            command = oldAction?.command ?: "",
            useRoot = oldAction?.useRoot ?: false,
            testResult = null,
        ),
    )
        private set

    fun onCommandChanged(newCommand: String) {
        state = state.copy(command = newCommand)
    }

    fun onUseRootChanged(newUseRoot: Boolean) {
        state = state.copy(useRoot = newUseRoot)
    }

    fun onTestClick() {
        viewModelScope.launch {
            state = state.copy(testResult = State.Loading)

            val result = if (state.useRoot) {
                suAdapter.executeWithOutput(state.command)
            } else {
                shellAdapter.executeWithOutput(state.command)
            }

            state = state.copy(
                testResult = when (result) {
                    is Success -> State.Data(result.data)
                    is KMError -> State.Data(result.getFullMessage(resourceProvider))
                },
            )
        }
    }

    fun onDoneClick() {
        val action = ActionData.ShellCommand(
            command = state.command,
            useRoot = state.useRoot,
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
