package io.github.sds100.keymapper.base.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.variables.VariableOperation
import io.github.sds100.keymapper.base.variables.VariableSuggestion
import io.github.sds100.keymapper.base.variables.VariablesUseCase
import io.github.sds100.keymapper.base.variables.filterVariableValue
import kotlinx.coroutines.flow.Flow

class SetVariableActionDelegate(
    private val variablesUseCase: VariablesUseCase,
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider,
    private val onResult: (ActionData.SetVariable) -> Unit,
) : ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider {

    var state: SetVariableActionBottomSheetState? by mutableStateOf(null)
        private set

    val suggestions: Flow<List<VariableSuggestion>> = variablesUseCase.suggestions

    fun open(oldAction: ActionData.SetVariable?) {
        state = SetVariableActionBottomSheetState(
            name = oldAction?.name ?: "",
            operation = oldAction?.operation ?: VariableOperation.ADD,
            value = oldAction?.value?.toString() ?: "1",
        )
    }

    fun onDismiss() {
        state = null
    }

    fun onNameChange(name: String) {
        state = state?.copy(name = name.trim())
    }

    fun onValueChange(value: String) {
        state = state?.copy(value = value.filterVariableValue())
    }

    fun onCycleOperationClick() {
        state = state?.let { it.copy(operation = it.operation.next()) }
    }

    fun onResetClick(name: String) {
        variablesUseCase.reset(name)
    }

    fun onResetAllClick() {
        variablesUseCase.resetAll()
    }

    fun onDoneClick() {
        val state = state ?: return
        val action = ActionData.SetVariable(
            name = state.name,
            operation = state.operation,
            value = state.value.toLongOrNull() ?: 0L,
        )

        onDismiss()

        onResult(action)
    }
}
