package io.github.sds100.keymapper.util.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Created by sds100 on 08/08/2022.
 */

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
    Column(modifier) {
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
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null
                    )
                }
            }
        )
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