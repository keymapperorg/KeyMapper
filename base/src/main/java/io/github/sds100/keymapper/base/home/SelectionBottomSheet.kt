package io.github.sds100.keymapper.base.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.groups.GroupBreadcrumbRow
import io.github.sds100.keymapper.base.groups.GroupListItemModel
import io.github.sds100.keymapper.base.groups.GroupRow
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.drawable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionBottomSheet(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    groups: List<GroupListItemModel>,
    breadcrumbs: List<GroupListItemModel>,
    selectedKeyMapsEnabled: SelectedKeyMapsEnabled,
    onDuplicateClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onEnabledKeyMapsChange: (Boolean) -> Unit = {},
    onNewGroupClick: () -> Unit = {},
    onGroupClick: (String?) -> Unit = {},
    showThisGroup: Boolean = false,
    onThisGroupClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier
            .widthIn(max = BottomSheetDefaults.SheetMaxWidth)
            .fillMaxWidth(),
        shadowElevation = 5.dp,
        shape = BottomSheetDefaults.ExpandedShape,
        tonalElevation = BottomSheetDefaults.Elevation,
        color = BottomSheetDefaults.ContainerColor,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .height(intrinsicSize = IntrinsicSize.Min),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(state = rememberScrollState()),
                ) {
                    Spacer(Modifier.width(16.dp))

                    SelectionButton(
                        text = stringResource(R.string.home_multi_select_duplicate),
                        icon = Icons.Rounded.ContentCopy,
                        enabled = enabled,
                        onClick = onDuplicateClick,
                    )

                    SelectionButton(
                        text = stringResource(R.string.home_multi_select_delete),
                        icon = Icons.Rounded.DeleteOutline,
                        enabled = enabled,
                        onClick = onDeleteClick,
                    )

                    SelectionButton(
                        text = stringResource(R.string.home_multi_select_export),
                        icon = Icons.Rounded.IosShare,
                        enabled = enabled,
                        onClick = onExportClick,
                    )

                    Spacer(Modifier.width(16.dp))
                }

                VerticalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                KeyMapsEnabledSwitch(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(IntrinsicSize.Max),
                    state = selectedKeyMapsEnabled,
                    enabled = enabled,
                    onCheckedChange = onEnabledKeyMapsChange,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.home_move_to_group),
                style = MaterialTheme.typography.labelLarge,
            )

            Spacer(modifier = Modifier.height(8.dp))

            GroupBreadcrumbRow(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                groups = breadcrumbs,
                onGroupClick = onGroupClick,
                enabled = true,
            )

            GroupRow(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                groups = groups,
                onNewGroupClick = onNewGroupClick,
                onGroupClick = onGroupClick,
                enabled = enabled,
                showThisGroupButton = showThisGroup,
                onThisGroupClick = onThisGroupClick,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SelectionButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier
            .padding(4.dp)
            .width(72.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                enabled = enabled,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onClick, interactionSource = interactionSource, enabled = enabled) {
            Icon(icon, text)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun KeyMapsEnabledSwitch(
    modifier: Modifier = Modifier,
    state: SelectedKeyMapsEnabled,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(
        modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Switch(
            checked = state == SelectedKeyMapsEnabled.ALL,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
        val text = when (state) {
            SelectedKeyMapsEnabled.ALL -> stringResource(R.string.home_enabled_key_maps_enabled)
            SelectedKeyMapsEnabled.NONE -> stringResource(R.string.home_enabled_key_maps_disabled)
            SelectedKeyMapsEnabled.MIXED -> stringResource(R.string.home_enabled_key_maps_mixed)
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )
    }
}

@Preview
@Composable
private fun PreviewEmptyGroups() {
    KeyMapperTheme {
        SelectionBottomSheet(
            enabled = true,
            groups = emptyList(),
            breadcrumbs = emptyList(),
            selectedKeyMapsEnabled = SelectedKeyMapsEnabled.ALL,
            onDuplicateClick = {},
            onDeleteClick = {},
            onExportClick = {},
            onEnabledKeyMapsChange = {},
        )
    }
}

@Preview
@Composable
private fun PreviewGroups() {
    val ctx = LocalContext.current

    KeyMapperTheme {
        SelectionBottomSheet(
            enabled = true,
            groups = listOf(
                GroupListItemModel(
                    uid = "1",
                    name = "Lockscreen",
                    icon = ComposeIconInfo.Vector(Icons.Outlined.Lock),
                ),
                GroupListItemModel(
                    uid = "2",
                    name = "Key Mapper",
                    icon = ComposeIconInfo.Drawable(ctx.drawable(R.mipmap.ic_launcher_round)),
                ),
                GroupListItemModel(
                    uid = "3",
                    name = "Key Mapper",
                    icon = null,
                ),
                GroupListItemModel(
                    uid = "3",
                    name = "Key Mapper",
                    icon = null,
                ),
                GroupListItemModel(
                    uid = "3",
                    name = "Key Mapper",
                    icon = null,
                ),
                GroupListItemModel(
                    uid = "3",
                    name = "Key Mapper",
                    icon = null,
                ),
                GroupListItemModel(
                    uid = "3",
                    name = "Key Mapper",
                    icon = null,
                ),
                GroupListItemModel(
                    uid = "3",
                    name = "Key Mapper",
                    icon = null,
                ),
            ),
            breadcrumbs = listOf(
                GroupListItemModel(
                    uid = "2",
                    name = "Key Mapper",
                    icon = null,
                ),
            ),
            selectedKeyMapsEnabled = SelectedKeyMapsEnabled.ALL,
            onDuplicateClick = {},
            onDeleteClick = {},
            onExportClick = {},
            onEnabledKeyMapsChange = {},
        )
    }
}
