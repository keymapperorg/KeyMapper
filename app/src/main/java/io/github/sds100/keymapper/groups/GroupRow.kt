package io.github.sds100.keymapper.groups

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    groups: List<SubGroupListModel>,
    onNewGroupClick: () -> Unit = {},
    onGroupClick: (String) -> Unit = {},
) {
    var viewAllState by rememberSaveable { mutableStateOf(false) }
    val transition =
        slideInVertically { height -> -height } togetherWith slideOutVertically { height -> height }
//
//    AnimatedContent(
//        viewAllState,
// //        transitionSpec = { transition },
//    ) { viewAll ->
    FlowRow(
        modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        maxLines = if (viewAllState) {
            Int.MAX_VALUE
        } else {
            2
        },
    ) {
        NewGroupButton(
            onClick = onNewGroupClick,
            text = stringResource(R.string.home_new_group_button),
            icon = {
                Icon(imageVector = Icons.Rounded.Add, null)
            },
            showText = groups.isEmpty(),
        )

        ViewAllButton(
            onClick = { viewAllState = !viewAllState },
            text = if (viewAllState) {
                stringResource(R.string.home_new_hide_groups_button)
            } else {
                stringResource(R.string.home_new_view_all_groups_button)
            },
        )

        for (group in groups) {
            GroupButton(
                onClick = { onGroupClick(group.uid) },
                text = group.name,
                icon = {
                    when (group.icon) {
                        is ComposeIconInfo.Drawable -> {
                            Icon(
                                painter = rememberDrawablePainter(group.icon.drawable),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified,
                            )
                        }

                        is ComposeIconInfo.Vector -> {
                            Icon(
                                imageVector = group.icon.imageVector,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        }

                        null -> {}
                    }
                },
            )
        }
//        }
    }
}

@Composable
private fun NewGroupButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    icon: @Composable () -> Unit,
    showText: Boolean = true,
) {
    Surface(
        modifier = modifier.height(36.dp),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
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
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ViewAllButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
) {
    Surface(
        modifier = modifier.height(36.dp),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
    ) {
        AnimatedContent(text) { text ->
            Text(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun GroupButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    icon: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.height(36.dp),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
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
        GroupRow(groups = emptyList())
    }
}

@Preview
@Composable
private fun PreviewOneItem() {
    KeyMapperTheme {
        GroupRow(
            groups = listOf(
                SubGroupListModel(
                    uid = "1",
                    name = "Device is locked",
                    icon = ComposeIconInfo.Vector(Icons.Outlined.Lock),
                ),
            ),
        )
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
                    SubGroupListModel(
                        uid = "1",
                        name = "Lockscreen",
                        icon = ComposeIconInfo.Vector(Icons.Outlined.Lock),
                    ),
                    SubGroupListModel(
                        uid = "2",
                        name = "Key Mapper",
                        icon = ComposeIconInfo.Drawable(ctx.drawable(R.mipmap.ic_launcher_round)),
                    ),
                    SubGroupListModel(
                        uid = "3",
                        name = "Key Mapper",
                        icon = null,
                    ),
                ),
            )
        }
    }
}
