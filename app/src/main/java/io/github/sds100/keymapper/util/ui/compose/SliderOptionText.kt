package io.github.sds100.keymapper.util.ui.compose

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderOptionText(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    defaultValue: Float? = null,
    valueText: (Float) -> String,
    isEnabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    stepSize: Int,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    if (showDialog) {
        ValueDialog(
            initialValue = value.roundToInt(),
            title = title,
            onDismissRequest = { showDialog = false },
            onSaveClick = {
                onValueChange(it.toFloat())
                showDialog = false
            },
        )
    }

    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(vertical = 8.dp),
            text = title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            val interactionSource = remember { MutableInteractionSource() }
            Slider(
                modifier = Modifier.weight(1f),
                value = value,
                onValueChange = onValueChange,
                enabled = isEnabled,
                valueRange = valueRange,
                interactionSource = interactionSource,
                thumb = { state ->
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        thumbSize = DpSize(4.dp, 28.dp),
                    )
                },
                steps = (((valueRange.endInclusive - valueRange.start) / stepSize.toFloat()).toInt()) - 1,
            )

            Spacer(modifier = Modifier.width(8.dp))

            ElevatedButton(onClick = { showDialog = true }) {
                Text(valueText(value))
            }

            if (defaultValue != null) {
                IconButton(onClick = { onValueChange(defaultValue) }) {
                    Icon(
                        Icons.Rounded.RestartAlt,
                        contentDescription = stringResource(R.string.slider_reset_content_description),
                    )
                }
            }
        }
    }
}

@Composable
private fun ValueDialog(
    initialValue: Int,
    title: String,
    onDismissRequest: () -> Unit,
    onSaveClick: (Int) -> Unit,
) {
    var newValue by rememberSaveable { mutableStateOf(initialValue.toString()) }
    val isError by remember { derivedStateOf { newValue.isBlank() || newValue.toIntOrNull() == null } }

    CustomDialog(
        title = title,
        confirmButton = {
            TextButton(
                onClick = { newValue.toIntOrNull()?.let { onSaveClick(it) } },
                enabled = !isError,
            ) {
                Text(stringResource(R.string.pos_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.neg_cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = newValue,
            onValueChange = { newValue = it },
            singleLine = true,
            maxLines = 1,
            isError = isError,
            supportingText = {
                when {
                    newValue.isBlank() -> {
                        Text(stringResource(R.string.error_cant_be_empty))
                    }

                    newValue.toIntOrNull() == null -> {
                        Text(stringResource(R.string.error_invalid_number))
                    }
                }
            },
        )
    }
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        Surface {
            SliderOptionText(
                modifier = Modifier.width(400.dp),
                title = "Repeat delay",
                value = 500f,
                defaultValue = 500f,
                valueText = { "${it.roundToInt()} ms" },
                isEnabled = true,
                onValueChange = {},
                valueRange = 0f..1000f,
                stepSize = 50,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDialog() {
    KeyMapperTheme {
        ValueDialog(
            initialValue = 500,
            title = "Repeat delay",
            onDismissRequest = {},
            onSaveClick = {},
        )
    }
}
