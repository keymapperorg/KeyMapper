package io.github.sds100.keymapper.base.constraints

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
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
import io.github.sds100.keymapper.base.utils.ui.compose.icons.Equal
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.variables.VariableComparison
import io.github.sds100.keymapper.base.variables.VariableConfigContent
import io.github.sds100.keymapper.base.variables.VariableSuggestion
import kotlinx.coroutines.launch

data class VariableConstraintSheetState(
    val name: String = "",
    val comparison: VariableComparison = VariableComparison.EQUALS,
    val value: String = "0",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariableConstraintBottomSheet(delegate: VariableConstraintDelegate) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val state = delegate.state
    val suggestions by delegate.suggestions.collectAsState(emptyList())

    if (state != null) {
        VariableConstraintBottomSheet(
            sheetState = sheetState,
            state = state,
            suggestions = suggestions,
            onDismissRequest = delegate::onDismiss,
            onNameChange = delegate::onNameChange,
            onValueChange = delegate::onValueChange,
            onCycleComparisonClick = delegate::onCycleComparisonClick,
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
private fun VariableConstraintBottomSheet(
    sheetState: SheetState,
    state: VariableConstraintSheetState,
    suggestions: List<VariableSuggestion> = emptyList(),
    onDismissRequest: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onValueChange: (String) -> Unit = {},
    onCycleComparisonClick: () -> Unit = {},
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
        title = stringResource(R.string.constraint_variable),
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
            nameError = nameError,
            onCycleOperationClick = onCycleComparisonClick,
            operationLabel = getComparisonLabel(state.comparison),
            operationIcon = {
                Icon(
                    imageVector = getComparisonIcon(state.comparison),
                    contentDescription = getComparisonLabel(state.comparison),
                )
            },
        )
    }
}

private fun getComparisonIcon(comparison: VariableComparison): ImageVector {
    return when (comparison) {
        VariableComparison.EQUALS -> KeyMapperIcons.Equal
        VariableComparison.GREATER_THAN -> Icons.Rounded.ChevronRight
        VariableComparison.LESS_THAN -> Icons.Rounded.ChevronLeft
    }
}

@Composable
private fun getComparisonLabel(comparison: VariableComparison): String {
    return when (comparison) {
        VariableComparison.EQUALS -> stringResource(R.string.variable_comparison_equals)
        VariableComparison.GREATER_THAN -> stringResource(R.string.variable_comparison_greater_than)
        VariableComparison.LESS_THAN -> stringResource(R.string.variable_comparison_less_than)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true, device = TABLET)
@Composable
private fun VariableConstraintBottomSheetPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )
        VariableConstraintBottomSheet(
            sheetState = sheetState,
            state = VariableConstraintSheetState(
                name = "counter",
                comparison = VariableComparison.GREATER_THAN,
                value = "3",
            ),
            suggestions = listOf(
                VariableSuggestion("counter", 3),
                VariableSuggestion("mode", 1),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun VariableConstraintBottomSheetEmptyPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )
        VariableConstraintBottomSheet(
            sheetState = sheetState,
            state = VariableConstraintSheetState(),
        )
    }
}
