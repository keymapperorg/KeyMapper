package io.github.sds100.keymapper.base.constraints

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.common.utils.SizeKM
import kotlinx.coroutines.launch

/**
 * State passed to the display resolution bottom sheet.
 *
 * @param supportedResolutions the resolutions the display reports as supported.
 * @param initialWidth the current real display width, used to prefill the custom fields.
 * @param initialHeight the current real display height, used to prefill the custom fields.
 */
data class DisplayResolutionSheetState(
    val supportedResolutions: List<SizeKM>,
    val initialWidth: Int,
    val initialHeight: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayResolutionConstraintBottomSheet(viewModel: ChooseConstraintViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state = viewModel.displayResolutionState ?: return

    DisplayResolutionConstraintBottomSheet(
        sheetState = sheetState,
        state = state,
        onDismissRequest = { viewModel.displayResolutionState = null },
        onDoneClick = { width, height ->
            viewModel.onDoneConfigDisplayResolutionClick(width, height)
        },
    )
}

/**
 * Compares two resolutions ignoring orientation so that e.g. 1080x1920 and 1920x1080
 * are treated as the same resolution.
 */
private fun SizeKM.matchesIgnoringOrientation(other: SizeKM): Boolean {
    return (width == other.width && height == other.height) ||
        (width == other.height && height == other.width)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplayResolutionConstraintBottomSheet(
    sheetState: SheetState,
    state: DisplayResolutionSheetState,
    onDismissRequest: () -> Unit = {},
    onDoneClick: (width: Int, height: Int) -> Unit = { _, _ -> },
) {
    val scope = rememberCoroutineScope()

    val initialSize = remember(state) { SizeKM(state.initialWidth, state.initialHeight) }
    val matchingResolution = remember(state) {
        state.supportedResolutions.firstOrNull { it.matchesIgnoringOrientation(initialSize) }
    }

    // Show the text fields immediately when there is nothing meaningful to pick from
    // or when the current resolution isn't one of the supported modes.
    var isCustom by remember(state) {
        mutableStateOf(state.supportedResolutions.size <= 1 || matchingResolution == null)
    }
    var selectedResolution by remember(state) {
        mutableStateOf(matchingResolution ?: state.supportedResolutions.firstOrNull())
    }
    var widthText by remember(state) { mutableStateOf(state.initialWidth.toString()) }
    var heightText by remember(state) { mutableStateOf(state.initialHeight.toString()) }

    val customWidth = widthText.toIntOrNull()
    val customHeight = heightText.toIntOrNull()

    val isValid = if (isCustom) {
        customWidth != null && customWidth > 0 && customHeight != null && customHeight > 0
    } else {
        selectedResolution != null
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.constraint_display_resolution_bottom_sheet_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // The chips are only useful when there is more than one supported mode to pick from.
            if (state.supportedResolutions.size > 1) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (resolution in state.supportedResolutions) {
                        FilterChip(
                            selected = !isCustom && selectedResolution == resolution,
                            onClick = {
                                isCustom = false
                                selectedResolution = resolution
                            },
                            label = { Text("${resolution.width} × ${resolution.height}") },
                        )
                    }

                    FilterChip(
                        selected = isCustom,
                        onClick = { isCustom = true },
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

            if (isCustom) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = widthText,
                        onValueChange = { widthText = it.filter(Char::isDigit) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = {
                            Text(stringResource(R.string.constraint_display_resolution_width))
                        },
                    )

                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = heightText,
                        onValueChange = { heightText = it.filter(Char::isDigit) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = {
                            Text(stringResource(R.string.constraint_display_resolution_height))
                        },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    },
                ) {
                    Text(stringResource(R.string.neg_cancel))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = isValid,
                    onClick = {
                        val width = if (isCustom) customWidth else selectedResolution?.width
                        val height = if (isCustom) customHeight else selectedResolution?.height

                        if (width != null && height != null) {
                            scope.launch {
                                sheetState.hide()
                                onDoneClick(width, height)
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.pos_done))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewWithModes() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        DisplayResolutionConstraintBottomSheet(
            sheetState = sheetState,
            state = DisplayResolutionSheetState(
                supportedResolutions = listOf(
                    SizeKM(1080, 2400),
                    SizeKM(1440, 3200),
                ),
                initialWidth = 1080,
                initialHeight = 2400,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewCustomOnly() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )

        DisplayResolutionConstraintBottomSheet(
            sheetState = sheetState,
            state = DisplayResolutionSheetState(
                supportedResolutions = emptyList(),
                initialWidth = 1080,
                initialHeight = 2400,
            ),
        )
    }
}
