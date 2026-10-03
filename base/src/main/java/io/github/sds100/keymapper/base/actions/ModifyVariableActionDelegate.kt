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

class ModifyVariableActionDelegate(
    private val variablesUseCase: VariablesUseCase,
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider,
    private val onResult: (ActionData.ModifyVariable) -> Unit,
) : ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider {

    var state: ModifyVariableActionBottomSheetState? by mutableStateOf(null)
        private set

    val suggestions: Flow<List<VariableSuggestion>> = variablesUseCase.suggestions

    fun open(oldAction: ActionData.ModifyVariable?) {
        state = ModifyVariableActionBottomSheetState(
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
        state = state?.let {
            val operation = when (it.operation) {
                VariableOperation.ADD -> VariableOperation.SUBTRACT
                else -> VariableOperation.ADD
            }
            it.copy(operation = operation)
        }
    }

    fun onResetClick(name: String) {
        variablesUseCase.reset(name)
    }

    fun onResetAllClick() {
        variablesUseCase.resetAll()
    }

    fun onDoneClick() {
        val state = state ?: return
        val action = ActionData.ModifyVariable(
            name = state.name,
            operation = state.operation,
            value = state.value.toLongOrNull() ?: 0L,
        )

        onDismiss()

        onResult(action)
    }
}
