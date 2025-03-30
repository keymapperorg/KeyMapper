package io.github.sds100.keymapper.groups

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.sds100.keymapper.R

@Composable
fun DeleteGroupDialog(
    modifier: Modifier = Modifier,
    groupName: String,
    onDismissRequest: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = {
            Text(stringResource(R.string.home_key_maps_delete_group_dialog_title, groupName))
        },
        text = {
            Text(
                stringResource(R.string.home_key_maps_delete_group_dialog_text),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDeleteClick) {
                Text(stringResource(R.string.home_key_maps_delete_group_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.home_key_maps_delete_group_cancel))
            }
        },
    )
}
