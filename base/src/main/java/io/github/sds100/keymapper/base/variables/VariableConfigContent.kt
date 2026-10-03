package io.github.sds100.keymapper.base.variables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VariableConfigContent(
    modifier: Modifier = Modifier,
    name: String,
    value: String,
    suggestions: List<VariableSuggestion>,
    nameError: String? = null,
    onNameChange: (String) -> Unit = {},
    onValueChange: (String) -> Unit = {},
    onSuggestionClick: (String) -> Unit = {},
    onResetClick: (String) -> Unit = {},
    onResetAllClick: () -> Unit = {},
    onCycleOperationClick: () -> Unit = {},
    operationIcon: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.widthIn(min = 128.dp),
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.variable_name_label)) },
                singleLine = true,
                isError = nameError != null,
                supportingText = if (nameError == null) {
                    null
                } else {
                    { Text(text = nameError, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrectEnabled = false,
                    capitalization = KeyboardCapitalization.None,
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            )

            if (operationIcon != null) {
                // One button that cycles through the operations rather than a row of them.
                FilledTonalIconButton(
                    // Material text fields expose no baseline, so centre the button on the 56dp
                    // field's text line instead. Top alignment keeps it put when error text shows.
                    modifier = Modifier.padding(top = 12.dp),
                    onClick = onCycleOperationClick,
                ) {
                    operationIcon()
                }
            }

            OutlinedTextField(
                modifier = Modifier.widthIn(min = 96.dp),
                value = value,
                onValueChange = onValueChange,
                label = { Text(stringResource(R.string.variable_value_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            )
        }

        if (suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.variable_suggestions),
                    style = MaterialTheme.typography.titleMedium,
                )

                TextButton(onClick = onResetAllClick) {
                    Text(stringResource(R.string.variable_reset_all))
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                for (suggestion in suggestions) {
                    InputChip(
                        selected = suggestion.name == name,
                        onClick = { onSuggestionClick(suggestion.name) },
                        label = {
                            Text(
                                text = stringResource(
                                    R.string.variable_chip_label,
                                    suggestion.name,
                                    suggestion.value.toString(),
                                ),
                            )
                        },
                        trailingIcon = {
                            Icon(
                                modifier = Modifier
                                    .size(InputChipDefaults.IconSize)
                                    .clickable { onResetClick(suggestion.name) },
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = stringResource(
                                    R.string.variable_reset_content_description,
                                    suggestion.name,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

private val previewSuggestions = listOf(
    VariableSuggestion("counter", 3),
    VariableSuggestion("mode", 1),
    VariableSuggestion("hits", 0),
)

@Composable
private fun Preview(
    modifier: Modifier = Modifier,
    name: String = "counter",
    value: String = "1",
    suggestions: List<VariableSuggestion> = previewSuggestions,
    nameError: String? = null,
) {
    KeyMapperTheme {
        VariableConfigContent(
            modifier = modifier.padding(16.dp),
            name = name,
            value = value,
            suggestions = suggestions,
            nameError = nameError,
            operationIcon = {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add",
                )
            },
        )
    }
}

@Preview
@Composable
private fun VariableConfigRowPreview() {
    KeyMapperTheme {
        Surface {
            Preview(suggestions = emptyList())
        }
    }
}

@Preview
@Composable
private fun VariableConfigRowNoSuggestionsPreview() {
    KeyMapperTheme {
        Surface {
            Preview(suggestions = emptyList())
        }
    }
}

@Preview
@Composable
private fun VariableConfigRowEmptyPreview() {
    KeyMapperTheme {
        Surface {
            Preview(name = "", value = "", suggestions = emptyList())
        }
    }
}

@Preview
@Composable
private fun VariableConfigRowErrorPreview() {
    KeyMapperTheme {
        Surface {
            Preview(name = "1counter", nameError = "Invalid variable name")
        }
    }
}

@Preview
@Composable
private fun VariableConfigRowTrailingPreview() {
    KeyMapperTheme {
        Surface {
            Preview(suggestions = emptyList())
        }
    }
}

@Preview
@Composable
private fun VariableConfigRowNarrowPreview() {
    KeyMapperTheme {
        Surface {
            Preview(
                modifier = Modifier.width(320.dp),
            )
        }
    }
}
