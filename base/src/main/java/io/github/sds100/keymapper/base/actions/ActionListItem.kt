package io.github.sds100.keymapper.base.actions

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.base.utils.drawable
import io.github.sds100.keymapper.base.utils.ui.LinkType
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.DragDropState

@Composable
fun ActionListItem(
    modifier: Modifier = Modifier,
    model: ActionListItemModel,
    index: Int,
    isDraggingEnabled: Boolean = false,
    isDragging: Boolean,
    isReorderingEnabled: Boolean,
    dragDropState: DragDropState? = null,
    onEditClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    onFixClick: () -> Unit = {},
    onTestClick: () -> Unit = {},
) {
    val draggableState = rememberDraggableState {
        dragDropState?.onDrag(Offset(0f, it))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .height(IntrinsicSize.Min)
                .padding(start = 16.dp, end = 16.dp)
                .draggable(
                    state = draggableState,
                    enabled = isDraggingEnabled,
                    orientation = Orientation.Vertical,
                    startDragImmediately = false,
                    onDragStarted = { offset ->
                        dragDropState?.onDragStart(index, offset)
                    },
                    onDragStopped = { dragDropState?.onDragInterrupted() },
                ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (isDragging) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(8.dp))

                if (isReorderingEnabled) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(Modifier.width(8.dp))

                if (model.error == null) {
                    when (model.icon) {
                        is ComposeIconInfo.Vector -> Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = model.icon.imageVector,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )

                        is ComposeIconInfo.Drawable -> {
                            val painter = rememberDrawablePainter(model.icon.drawable)
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painter,
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )
                        }
                    }
                }

                val primaryText = model.text

                Spacer(Modifier.width(8.dp))

                TextColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    primaryText = primaryText,
                    secondaryText = model.secondaryText,
                    errorText = model.error,
                )

                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides 16.dp,
                ) {
                    if (model.error != null && model.isErrorFixable) {
                        FilledTonalButton(
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                            onClick = onFixClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.button_fix),
                            )
                        }
                    }

                    if (model.error == null) {
                        IconButton(onClick = onTestClick) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircleOutline,
                                contentDescription = stringResource(R.string.action_list_item_test),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.action_list_item_edit),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    IconButton(onClick = onRemoveClick) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = stringResource(R.string.action_list_item_remove),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        if (model.linkType == LinkType.HIDDEN) {
            // Important! Show an empty spacer so the height of the card remains constant
            // while dragging. If the height changes while dragging it can lead to janky
            // behavior.
            Spacer(Modifier.height(32.dp))
        } else {
            Spacer(Modifier.height(4.dp))

            Icon(
                imageVector = when (model.linkType) {
                    LinkType.ARROW -> Icons.Rounded.ArrowDownward
                    LinkType.PLUS -> Icons.Rounded.Add
                    LinkType.HIDDEN -> Icons.Rounded.Add
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun TextColumn(
    modifier: Modifier = Modifier,
    primaryText: String,
    secondaryText: String? = null,
    errorText: String? = null,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = primaryText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (secondaryText != null) {
            Text(
                text = secondaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview
@Composable
private fun NoDragPreview() {
    ActionListItem(
        model = ActionListItemModel(
            id = "id",
            text = "Dismiss most recent notification",
            secondaryText = "Repeat until released",
            error = "Denied notification access permission",
            isErrorFixable = true,
            icon = ComposeIconInfo.Vector(Icons.Outlined.ClearAll),
        ),
        isDragging = false,
        isReorderingEnabled = false,
        index = 0,
    )
}

@Preview
@Composable
private fun NoDragOneLinePreview() {
    ActionListItem(
        model = ActionListItemModel(
            id = "id",
            text = "Clear all",
            secondaryText = null,
            error = null,
            isErrorFixable = true,
            icon = ComposeIconInfo.Vector(Icons.Outlined.ClearAll),
        ),
        isDragging = false,
        isReorderingEnabled = false,
        index = 0,
    )
}

@Preview
@Composable
private fun DragDrawablePreview() {
    val drawable = LocalContext.current.drawable(R.mipmap.ic_launcher_round)

    ActionListItem(
        model = ActionListItemModel(
            id = "id",
            text = "Dismiss most recent notification",
            secondaryText = "Repeat until released",
            error = null,
            isErrorFixable = true,
            icon = ComposeIconInfo.Drawable(drawable),
        ),
        isDragging = false,
        isReorderingEnabled = true,
        index = 0,
    )
}
