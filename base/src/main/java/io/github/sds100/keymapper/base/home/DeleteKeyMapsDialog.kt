package io.github.sds100.keymapper.base.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.sds100.keymapper.R

@Composable
fun DeleteKeyMapsDialog(
    modifier: Modifier = Modifier,
    keyMapCount: Int,
    onDismissRequest: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                pluralStringResource(
                    R.plurals.home_key_maps_delete_dialog_title,
                    keyMapCount,
                    keyMapCount,
                ),
            )
        },
        text = {
            Text(
                stringResource(R.string.home_key_maps_delete_dialog_text, keyMapCount),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDeleteClick) {
                Text(stringResource(R.string.home_key_maps_delete_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.home_key_maps_delete_cancel))
            }
        },
    )
}
