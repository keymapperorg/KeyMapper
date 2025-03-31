package io.github.sds100.keymapper.groups

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo

@Composable
fun GroupRow(
    modifier: Modifier = Modifier,
    groups: List<GroupListItemModel>,
    onNewGroupClick: () -> Unit = {},
    onGroupClick: (String) -> Unit = {},
    enabled: Boolean = true,
    isSubgroups: Boolean = false,
) {
    var viewAllState by rememberSaveable { mutableStateOf(false) }

    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier
            .verticalScroll(rememberScrollState())
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        maxLines = if (viewAllState) {
            Int.MAX_VALUE
        } else {
            2
        },
        overflow = FlowRowOverflow.expandOrCollapseIndicator(
            expandIndicator = {
                ViewAllButton(
                    onClick = { viewAllState = true },
                    text = stringResource(R.string.home_new_view_all_groups_button),
                    enabled = enabled,
                )
            },
            collapseIndicator = {
                ViewAllButton(
                    onClick = { viewAllState = false },
                    text = stringResource(R.string.home_new_hide_groups_button),
                    enabled = enabled,
                )
            },
            minRowsToShowCollapse = 3,
        ),
    ) {
        NewGroupButton(
            onClick = onNewGroupClick,
            text = if (isSubgroups) {
                stringResource(R.string.home_new_subgroup_button)
            } else {
                stringResource(R.string.home_new_group_button)
            },
            icon = {
                Icon(imageVector = Icons.Rounded.Add, null)
            },
            showText = groups.isEmpty(),
            enabled = enabled,
        )

        for (group in groups) {
            GroupButton(
                onClick = { onGroupClick(group.uid) },
                text = group.name,
                enabled = enabled,
                icon = {
                    when (group.icon) {
                        is ComposeIconInfo.Drawable -> {
                            Icon(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp),
                                painter = rememberDrawablePainter(group.icon.drawable),
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )
                        }

                        is ComposeIconInfo.Vector -> {
                            Icon(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp),
                                imageVector = group.icon.imageVector,
                                contentDescription = null,
                            )
                        }

                        null -> {}
                    }
                },
            )
        }
    }
}

@Composable
private fun NewGroupButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    icon: @Composable () -> Unit,
    showText: Boolean = true,
    enabled: Boolean,
) {
    val color = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    CompositionLocalProvider(
        LocalContentColor provides color,
    ) {
        Surface(
            modifier = modifier.height(36.dp),
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, color = color),
            color = Color.Transparent,
            enabled = enabled,
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon()

                if (showText) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewAllButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    enabled: Boolean,
) {
    val color = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    CompositionLocalProvider(
        LocalContentColor provides color,
    ) {
        Surface(
            modifier = modifier,
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, color),
            color = Color.Transparent,
            enabled = enabled,
        ) {
            AnimatedContent(text) { text ->
                Text(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun GroupButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()

            Text(
                text = text,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewEmpty() {
    KeyMapperTheme {
        Surface {
            GroupRow(groups = emptyList())
        }
    }
}

@Preview
@Composable
private fun PreviewEmptyDisabled() {
    KeyMapperTheme {
        Surface {
            GroupRow(groups = emptyList(), enabled = false)
        }
    }
}

@Preview
@Composable
private fun PreviewOneItem() {
    KeyMapperTheme {
        Surface {
            GroupRow(
                groups = listOf(
                    GroupListItemModel(
                        uid = "1",
                        name = "Device is locked",
                        icon = ComposeIconInfo.Vector(Icons.Outlined.Lock),
                    ),
                ),
                enabled = false,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewMultipleItems() {
    val ctx = LocalContext.current

    KeyMapperTheme {
        Surface {
            GroupRow(
                groups = listOf(
                    GroupListItemModel(
                        uid = "1",
                        name = "Lockscreen",
                        icon = ComposeIconInfo.Vector(Icons.Outlined.Lock),
                    ),
                    GroupListItemModel(
                        uid = "2",
                        name = "Lockscreen",
                        icon = ComposeIconInfo.Vector(Icons.Outlined.Lock),
                    ),
                    GroupListItemModel(
                        uid = "3",
                        name = "Lockscreen",
                        icon = ComposeIconInfo.Vector(Icons.Outlined.Lock),
                    ),
                    GroupListItemModel(
                        uid = "1",
                        name = "Lockscreen",
                        icon = ComposeIconInfo.Vector(Icons.Outlined.Lock),
                    ),
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
                ),
            )
        }
    }
}
