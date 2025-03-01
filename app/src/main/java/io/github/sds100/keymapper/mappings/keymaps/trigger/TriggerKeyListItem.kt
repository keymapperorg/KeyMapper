package io.github.sds100.keymapper.mappings.keymaps.trigger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.R

@Composable
fun TriggerKeyListItem(
    modifier: Modifier = Modifier,
    model: TriggerKeyListItemState,
    onEditClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Icon(
            imageVector = when (model.linkType) {
                TriggerKeyLinkType.ARROW -> Icons.Filled.ArrowDownward
                TriggerKeyLinkType.PLUS -> Icons.Filled.Add
                TriggerKeyLinkType.HIDDEN -> null // Handle hidden case
            } ?: return@Column, // Exit if linkType is HIDDEN
            contentDescription = "Link Type",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp),
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (model.isDragDropEnabled) {
                    IconButton(onClick = { /* Handle drag */ }) {
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = "Drag",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                ) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (model.clickTypeString != null) {
                        Text(
                            text = model.clickTypeString,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (model.extraInfo != null) {
                        Text(
                            text = model.extraInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.trigger_key_list_item_edit),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp),
                    )
                }

                IconButton(onClick = onRemoveClick) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.trigger_key_list_item_remove),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun TriggerKeyListItemPreview() {
    TriggerKeyListItem(
        model = TriggerKeyListItemState(
            id = "id",
            name = "Volume Up",
            clickTypeString = "Long Press",
            extraInfo = "External Keyboard",
            linkType = TriggerKeyLinkType.ARROW,
            isDragDropEnabled = true,
        ),
    )
}
