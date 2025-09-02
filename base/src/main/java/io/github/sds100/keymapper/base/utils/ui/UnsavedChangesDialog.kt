package io.github.sds100.keymapper.base.utils.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme

@Composable
fun UnsavedChangesDialog(
    onDismiss: () -> Unit,
    onDiscardClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_unsaved_changes)) },
        text = { Text(stringResource(R.string.dialog_message_unsaved_changes)) },
        confirmButton = {
            TextButton(onClick = onDiscardClick) { Text(stringResource(R.string.pos_discard_changes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.neg_keep_editing)) }
        },
    )
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        UnsavedChangesDialog(onDismiss = {}, onDiscardClick = {})
    }
}
