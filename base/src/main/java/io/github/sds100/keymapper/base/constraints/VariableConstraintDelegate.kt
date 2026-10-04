package io.github.sds100.keymapper.base.constraints

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.sds100.keymapper.base.variables.VariableComparison
import io.github.sds100.keymapper.base.variables.VariablesUseCase
import io.github.sds100.keymapper.base.variables.filterVariableValue

class VariableConstraintDelegate(
    private val variablesUseCase: VariablesUseCase,
    private val onResult: (ConstraintData.Variable) -> Unit,
) {
    var state: VariableConstraintSheetState? by mutableStateOf(null)
        private set

    val suggestions = variablesUseCase.suggestions

    fun open(oldConstraint: ConstraintData.Variable?) {
        state = VariableConstraintSheetState(
            name = oldConstraint?.name ?: "",
            comparison = oldConstraint?.comparison ?: VariableComparison.EQUALS,
            value = oldConstraint?.value?.toString() ?: "0",
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

    fun onCycleComparisonClick() {
        state = state?.let { it.copy(comparison = it.comparison.next()) }
    }

    fun onResetClick(name: String) {
        variablesUseCase.reset(name)
    }

    fun onResetAllClick() {
        variablesUseCase.resetAll()
    }

    fun onDoneClick() {
        val state = state ?: return
        val constraint = ConstraintData.Variable(
            name = state.name,
            comparison = state.comparison,
            value = state.value.toLongOrNull() ?: 0L,
        )
        onDismiss()
        onResult(constraint)
    }
}
