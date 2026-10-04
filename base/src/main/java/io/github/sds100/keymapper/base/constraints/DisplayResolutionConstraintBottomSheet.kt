package io.github.sds100.keymapper.base.constraints

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.KMBottomSheet
import io.github.sds100.keymapper.common.utils.SizeKM
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayResolutionConstraintBottomSheet(delegate: CreateConstraintDelegate) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state = delegate.displayResolutionState ?: return

    DisplayResolutionConstraintBottomSheet(
        sheetState = sheetState,
        state = state,
        onSelectResolution = delegate::onSelectDisplayResolution,
        onSelectCustom = delegate::onSelectCustomDisplayResolution,
        onWidthChange = delegate::onDisplayResolutionWidthChange,
        onHeightChange = delegate::onDisplayResolutionHeightChange,
        onDismissRequest = delegate::onDismissDisplayResolution,
        onDoneClick = delegate::onDoneConfigDisplayResolutionClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplayResolutionConstraintBottomSheet(
    sheetState: SheetState,
    state: DisplayResolutionSheetState,
    onSelectResolution: (SizeKM) -> Unit = {},
    onSelectCustom: () -> Unit = {},
    onWidthChange: (String) -> Unit = {},
    onHeightChange: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    KMBottomSheet(
        title = stringResource(R.string.constraint_display_resolution_bottom_sheet_title),
        negButtonText = stringResource(R.string.neg_cancel),
        posButtonText = stringResource(R.string.pos_done),
        onPosButtonClick = {
            scope.launch {
                sheetState.hide()
                onDoneClick()
            }
        },
        posButtonEnabled = state.isValid,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        // The chips are only useful when there is more than one supported mode to pick from.
        if (state.supportedResolutions.size > 1) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (resolution in state.supportedResolutions) {
                    FilterChip(
                        selected = !state.isCustom && state.selectedResolution == resolution,
                        onClick = { onSelectResolution(resolution) },
                        label = { Text("${resolution.width} × ${resolution.height}") },
                    )
                }

                FilterChip(
                    selected = state.isCustom,
                    onClick = onSelectCustom,
                    label = {
                        Text(
                            stringResource(
                                R.string.constraint_display_resolution_custom_chip,
                            ),
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (state.isCustom) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.widthText,
                    onValueChange = onWidthChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = {
                        Text(stringResource(R.string.constraint_display_resolution_width))
                    },
                )

                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.heightText,
                    onValueChange = onHeightChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = {
                        Text(stringResource(R.string.constraint_display_resolution_height))
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun PreviewWithModes() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )

        DisplayResolutionConstraintBottomSheet(
            sheetState = sheetState,
            state = DisplayResolutionSheetState(
                supportedResolutions = listOf(
                    SizeKM(1080, 2400),
                    SizeKM(1440, 3200),
                ),
                isCustom = false,
                selectedResolution = SizeKM(1080, 2400),
                widthText = "1080",
                heightText = "2400",
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun PreviewCustomOnly() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )

        DisplayResolutionConstraintBottomSheet(
            sheetState = sheetState,
            state = DisplayResolutionSheetState(
                supportedResolutions = emptyList(),
                isCustom = true,
                selectedResolution = null,
                widthText = "1080",
                heightText = "2400",
            ),
        )
    }
}
