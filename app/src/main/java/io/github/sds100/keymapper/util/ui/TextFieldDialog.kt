package io.github.sds100.keymapper.util.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.sds100.keymapper.R

@Composable
fun TextFieldDialog(
    title: String,
    label: String = "",
    error: String? = null,
    onTextChange: (String) -> Unit = {},
    onConfirm: (String) -> Unit = { _ -> },
    onDismiss: () -> Unit = {}
) {
    var text: String by rememberSaveable { mutableStateOf("") }

    CustomDialog(
        title = title,
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = error == null) {
                Text(stringResource(R.string.pos_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.neg_cancel))
            }
        },
        onDismissRequest = onDismiss
    ) {
        ErrorTextField(
            text = text,
            label = label,
            errorMessage = error ?: "",
            isError = error != null,
            onValueChange = {
                text = it
                onTextChange(it)
            }
        )
    }
}

@Preview
@Composable
private fun TextFieldDialogPreview() {
    MaterialTheme {
        TextFieldDialog(
            title = "Title",
            label = "Label"
        )
    }
}