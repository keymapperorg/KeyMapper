package io.github.sds100.keymapper.base.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.backup.BackupFileListItem
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.CustomDialog

@Composable
fun BackupFilePickerDialog(
    files: List<BackupFileListItem>,
    canRequestFullAccess: Boolean,
    onFileClick: (String) -> Unit,
    onRequestFullAccessClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    CustomDialog(
        title = stringResource(R.string.home_import_choose_backup_dialog_title),
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.neg_cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (files.isEmpty()) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    text = stringResource(R.string.home_import_no_backups_found_in_downloads),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn {
                    items(files, key = { it.uri }) { file ->
                        BackupFileListRow(
                            modifier = Modifier.fillMaxWidth(),
                            name = file.name,
                            onClick = { onFileClick(file.uri) },
                        )
                    }
                }
            }

            if (canRequestFullAccess) {
                FilledTonalButton(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .align(Alignment.CenterHorizontally),
                    onClick = onRequestFullAccessClick,
                ) {
                    Text(stringResource(R.string.home_import_grant_full_access_button))
                }
            }
        }
    }
}

@Composable
private fun BackupFileListRow(modifier: Modifier = Modifier, name: String, onClick: () -> Unit) {
    Surface(modifier = modifier, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = null)
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview
@Composable
private fun PreviewBackupFilePickerDialog() {
    KeyMapperTheme {
        BackupFilePickerDialog(
            files = listOf(
                BackupFileListItem(uri = "content://0", name = "key_maps_20260101-120000.zip"),
                BackupFileListItem(uri = "content://1", name = "key_maps_20260215-093000.zip"),
            ),
            canRequestFullAccess = true,
            onFileClick = {},
            onRequestFullAccessClick = {},
            onDismissRequest = {},
        )
    }
}

@Preview
@Composable
private fun PreviewBackupFilePickerDialogEmpty() {
    KeyMapperTheme {
        BackupFilePickerDialog(
            files = emptyList(),
            canRequestFullAccess = false,
            onFileClick = {},
            onRequestFullAccessClick = {},
            onDismissRequest = {},
        )
    }
}
