package io.github.sds100.keymapper.util.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Created by sds100 on 08/08/2022.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorOutlinedTextField(
    modifier: Modifier = Modifier,
    text: String,
    label: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    errorMessage: String,
    isError: Boolean,
    onValueChange: (String) -> Unit = {}
) {
    ErrorTextField(
        modifier = modifier,
        textField = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                onValueChange = onValueChange,
                singleLine = true,
                label = { Text(label) },
                isError = isError,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                trailingIcon = {
                    if (isError) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        errorMessage = errorMessage,
        isError = isError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorTextField(
    modifier: Modifier = Modifier,
    text: String,
    label: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    errorMessage: String,
    isError: Boolean,
    onValueChange: (String) -> Unit = {}
) {
    ErrorTextField(
        modifier = modifier,
        textField = {
            TextField(
                value = text,
                onValueChange = onValueChange,
                singleLine = true,
                label = { Text(label) },
                isError = isError,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                trailingIcon = {
                    if (isError) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        errorMessage = errorMessage,
        isError = isError
    )
}

@Composable
private fun ErrorTextField(
    modifier: Modifier,
    textField: @Composable () -> Unit,
    errorMessage: String,
    isError: Boolean,
) {
    Column(modifier) {
        textField()

        // Supporting text for error message.
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(start = 16.dp, top = 4.dp)
                .alpha(if (isError) 1f else 0f)
        )
    }
}

@Preview
@Composable
private fun Preview() {
    MaterialTheme {
        Surface {
            ErrorTextField(
                text = "Text",
                label = "Label",
                errorMessage = "Error",
                isError = true
            )
        }
    }
}