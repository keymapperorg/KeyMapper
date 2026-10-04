package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices.TABLET
import androidx.compose.ui.tooling.preview.Preview
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.KMBottomSheet
import io.github.sds100.keymapper.base.variables.VariableConfigContent
import io.github.sds100.keymapper.base.variables.VariableOperation
import io.github.sds100.keymapper.base.variables.VariableSuggestion
import kotlinx.coroutines.launch

data class ModifyVariableActionBottomSheetState(
    val name: String = "",
    val operation: VariableOperation = VariableOperation.ADD,
    val value: String = "1",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifyVariableActionBottomSheet(delegate: ModifyVariableActionDelegate) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state = delegate.state
    val suggestions by delegate.suggestions.collectAsState(emptyList())

    if (state != null) {
        ModifyVariableActionBottomSheet(
            sheetState = sheetState,
            state = state,
            suggestions = suggestions,
            onDismissRequest = delegate::onDismiss,
            onNameChange = delegate::onNameChange,
            onValueChange = delegate::onValueChange,
            onCycleOperationClick = delegate::onCycleOperationClick,
            onResetClick = delegate::onResetClick,
            onResetAllClick = delegate::onResetAllClick,
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    delegate.onDoneClick()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModifyVariableActionBottomSheet(
    sheetState: SheetState,
    state: ModifyVariableActionBottomSheetState,
    suggestions: List<VariableSuggestion> = emptyList(),
    onDismissRequest: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onValueChange: (String) -> Unit = {},
    onCycleOperationClick: () -> Unit = {},
    onResetClick: (String) -> Unit = {},
    onResetAllClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    val nameEmptyError = stringResource(R.string.variable_name_error)
    var nameError: String? by rememberSaveable { mutableStateOf(null) }

    if (state.name.isNotBlank() && nameError != null) {
        nameError = null
    }

    KMBottomSheet(
        title = stringResource(R.string.action_modify_variable),
        negButtonText = stringResource(R.string.neg_cancel),
        posButtonText = stringResource(R.string.pos_done),
        onPosButtonClick = {
            if (state.name.isBlank()) {
                nameError = nameEmptyError
            } else {
                onDoneClick()
            }
        },
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        VariableConfigContent(
            modifier = Modifier.fillMaxWidth(),
            name = state.name,
            onNameChange = onNameChange,
            value = state.value,
            onValueChange = onValueChange,
            suggestions = suggestions,
            onSuggestionClick = onNameChange,
            onResetClick = onResetClick,
            onResetAllClick = onResetAllClick,
            onCycleOperationClick = onCycleOperationClick,
            nameError = nameError,
            operationLabel = getOperationLabel(state.operation),
            operationIcon = {
                Icon(
                    imageVector = getOperationIcon(state.operation),
                    contentDescription = getOperationLabel(state.operation),
                )
            },
        )
    }
}

private fun getOperationIcon(operation: VariableOperation): ImageVector {
    return when (operation) {
        VariableOperation.ADD -> Icons.Rounded.Add
        VariableOperation.SUBTRACT -> Icons.Rounded.Remove
    }
}

@Composable
private fun getOperationLabel(operation: VariableOperation): String {
    return when (operation) {
        VariableOperation.ADD -> stringResource(R.string.variable_operation_add)
        VariableOperation.SUBTRACT -> stringResource(R.string.variable_operation_subtract)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true, device = TABLET)
@Composable
private fun ModifyVariableActionBottomSheetPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )
        ModifyVariableActionBottomSheet(
            sheetState = sheetState,
            state = ModifyVariableActionBottomSheetState(
                name = "counter",
                operation = VariableOperation.ADD,
                value = "1",
            ),
            suggestions = listOf(
                VariableSuggestion("counter", 3),
                VariableSuggestion("mode", 1),
                VariableSuggestion("hits", 0),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun ModifyVariableActionBottomSheetEmptyPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )
        ModifyVariableActionBottomSheet(
            sheetState = sheetState,
            state = ModifyVariableActionBottomSheetState(),
        )
    }
}
