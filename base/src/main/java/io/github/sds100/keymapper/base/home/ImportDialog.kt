package io.github.sds100.keymapper.base.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme

@Composable
fun ImportDialog(
    modifier: Modifier = Modifier,
    keyMapCount: Int,
    onDismissRequest: () -> Unit,
    onAppendClick: () -> Unit,
    onReplaceClick: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                pluralStringResource(
                    R.plurals.home_importing_dialog_title,
                    keyMapCount,
                    keyMapCount,
                ),
            )
        },
        text = {
            Text(
                stringResource(R.string.home_importing_dialog_text, keyMapCount),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onAppendClick) {
                Text(stringResource(R.string.home_importing_dialog_append))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.home_importing_dialog_cancel))
            }

            TextButton(onClick = onReplaceClick) {
                Text(stringResource(R.string.home_importing_dialog_replace))
            }
        },
    )
}

@Preview
@Composable
private fun ImportDialogPreview() {
    KeyMapperTheme {
        ImportDialog(
            keyMapCount = 3,
            onDismissRequest = {},
            onAppendClick = {},
            onReplaceClick = {},
        )
    }
}
