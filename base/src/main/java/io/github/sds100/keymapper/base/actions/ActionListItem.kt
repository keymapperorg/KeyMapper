package io.github.sds100.keymapper.base.actions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.CompactErrorButton
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.DragDropState
import io.github.sds100.keymapper.base.utils.ui.compose.EXPAND_ANIMATION_DURATION
import io.github.sds100.keymapper.base.utils.ui.compose.ExpandableDraggableCard
import io.github.sds100.keymapper.base.utils.ui.compose.horizontalFadingEdges
import io.github.sds100.keymapper.base.utils.ui.drawable

/**
 * The Material 3 alpha for disabled content.
 */
private const val DISABLED_ALPHA = 0.38f

/**
 * The summary fades out in the first half of the animation so it is invisible before the shrink
 * clips it. When expanding, the space is made first and the text fades in during the second half.
 */
private val summaryEnterTransition: EnterTransition =
    expandVertically(
        animationSpec = tween(EXPAND_ANIMATION_DURATION, easing = FastOutSlowInEasing),
        expandFrom = Alignment.Top,
    ) + fadeIn(
        tween(
            durationMillis = EXPAND_ANIMATION_DURATION / 2,
            delayMillis = EXPAND_ANIMATION_DURATION / 2,
            easing = FastOutSlowInEasing,
        ),
    )

private val summaryExitTransition: ExitTransition =
    fadeOut(tween(EXPAND_ANIMATION_DURATION / 2, easing = FastOutSlowInEasing)) +
        shrinkVertically(
            animationSpec = tween(EXPAND_ANIMATION_DURATION, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top,
        )

/**
 * Animate both width and height so the title row does not jump when the rename button appears.
 */
private val renameButtonEnterTransition: EnterTransition =
    fadeIn(tween(EXPAND_ANIMATION_DURATION, easing = FastOutSlowInEasing)) +
        expandIn(
            animationSpec = tween(EXPAND_ANIMATION_DURATION, easing = FastOutSlowInEasing),
            expandFrom = Alignment.CenterStart,
        )

private val renameButtonExitTransition: ExitTransition =
    fadeOut(tween(EXPAND_ANIMATION_DURATION, easing = FastOutSlowInEasing)) +
        shrinkOut(
            animationSpec = tween(EXPAND_ANIMATION_DURATION, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.CenterStart,
        )

@Composable
fun ActionListItem(
    modifier: Modifier = Modifier,
    model: ActionListItemModel,
    index: Int,
    isExpanded: Boolean,
    isDraggingEnabled: Boolean = false,
    isDragging: Boolean,
    isReorderingEnabled: Boolean,
    dragDropState: DragDropState? = null,
    onExpandedChange: (Boolean) -> Unit = {},
    onEditClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    onFixClick: () -> Unit = {},
    onTestClick: () -> Unit = {},
    onDuplicateClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onEnabledChange: (Boolean) -> Unit = {},
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    // Only grey out a disabled action when it is collapsed so the expanded options stay readable.
    val contentAlpha by animateFloatAsState(
        targetValue = if (model.isEnabled || isExpanded) 1f else DISABLED_ALPHA,
        animationSpec = tween(EXPAND_ANIMATION_DURATION, easing = FastOutSlowInEasing),
    )

    ExpandableDraggableCard(
        modifier = modifier,
        key = model.id,
        isExpanded = isExpanded,
        isDragging = isDragging,
        isReorderingEnabled = isReorderingEnabled,
        isDraggingEnabled = isDraggingEnabled,
        dragDropState = dragDropState,
        dragHandleContentDescription = stringResource(R.string.drag_handle_for, model.title),
        expandContentDescription = stringResource(R.string.action_list_item_expand),
        collapseContentDescription = stringResource(R.string.action_list_item_collapse),
        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
        onExpandedChange = onExpandedChange,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        headerContent = { expanded ->
            ActionIcon(modifier = Modifier.size(20.dp), icon = model.icon)

            Spacer(Modifier.width(8.dp))

            HeaderText(
                modifier = Modifier.weight(1f).animateContentSize().padding(vertical = 8.dp),
                model = model,
                isExpanded = expanded,
                onRenameClick = onRenameClick,
            )
        },
        expandedContent = {
            ExpandedContent(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 8.dp,
                    bottom = 8.dp,
                    top = 8.dp,
                ),
                model = model,
                onEditClick = onEditClick,
                onRemoveClick = onRemoveClick,
                onFixClick = onFixClick,
                onTestClick = onTestClick,
                onDuplicateClick = onDuplicateClick,
                onEnabledChange = onEnabledChange,
            )
        },
    )
}

@Composable
private fun ActionIcon(modifier: Modifier = Modifier, icon: ComposeIconInfo) {
    when (icon) {
        is ComposeIconInfo.Vector -> Icon(
            modifier = modifier,
            imageVector = icon.imageVector,
            contentDescription = null,
        )

        is ComposeIconInfo.Drawable -> {
            val painter = rememberDrawablePainter(icon.drawable)
            Icon(
                modifier = modifier,
                painter = painter,
                contentDescription = null,
                tint = Color.Unspecified,
            )
        }
    }
}

@Composable
private fun HeaderText(
    modifier: Modifier = Modifier,
    model: ActionListItemModel,
    isExpanded: Boolean,
    onRenameClick: () -> Unit,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier
                    .weight(1f, fill = false),
                text = model.title,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = if (model.isCustomName) FontStyle.Italic else FontStyle.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = renameButtonEnterTransition,
                exit = renameButtonExitTransition,
            ) {
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides 16.dp,
                ) {
                    IconButton(onClick = onRenameClick) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.action_list_item_rename),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !isExpanded,
            enter = summaryEnterTransition,
            exit = summaryExitTransition,
        ) {
            Column {
                if (model.summary != null) {
                    Text(
                        text = model.summary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (model.error != null && model.isEnabled) {
                    Text(
                        text = model.error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedContent(
    modifier: Modifier = Modifier,
    model: ActionListItemModel,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onFixClick: () -> Unit,
    onTestClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (model.error != null) {
            Row(
                modifier = Modifier.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = model.error,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )

                if (model.isErrorFixable) {
                    Spacer(Modifier.width(8.dp))

                    CompactErrorButton(onClick = onFixClick) {
                        Text(stringResource(R.string.button_fix))
                    }
                }
            }
        }

        if (model.showRepeat) {
            OptionRow(
                modifier = Modifier
                    .fillMaxWidth(),
                icon = Icons.Rounded.Repeat,
                text = model.repeatText ?: stringResource(R.string.action_list_no_repeat),
                isSet = model.repeatText != null,
            )
        }

        OptionRow(
            modifier = Modifier
                .fillMaxWidth(),
            icon = Icons.Rounded.KeyboardDoubleArrowRight,
            text = model.burstText ?: stringResource(R.string.action_list_no_burst),
            isSet = model.burstText != null,
        )

        if (model.showHoldDown) {
            OptionRow(
                modifier = Modifier
                    .fillMaxWidth(),
                icon = Icons.Rounded.TouchApp,
                text = model.holdDownText ?: stringResource(R.string.action_list_no_hold_down),
                isSet = model.holdDownText != null,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.switch_enabled),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.width(16.dp))

            Switch(checked = model.isEnabled, onCheckedChange = onEnabledChange)

            Spacer(Modifier.width(16.dp))

            val scrollState = rememberScrollState()

            // Scroll to the end initially so the delete button is always visible without
            // needing to scroll.
            LaunchedEffect(Unit) {
                scrollState.scrollTo(scrollState.maxValue)
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalFadingEdges(scrollState)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.End,
            ) {
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides 16.dp,
                ) {
                    IconButton(onClick = onDuplicateClick) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(
                                R.string.action_list_item_duplicate,
                            ),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    IconButton(onClick = onTestClick) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = stringResource(R.string.action_list_item_test),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.action_list_item_edit),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    IconButton(onClick = onRemoveClick) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.action_list_item_remove),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    isSet: Boolean,
) {
    val color = if (isSet) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = icon,
            contentDescription = null,
            tint = color,
        )

        Spacer(Modifier.width(16.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Preview
@Composable
private fun CollapsedErrorPreview() {
    KeyMapperTheme {
        ActionListItem(
            model = ActionListItemModel(
                id = "id",
                title = "Dismiss most recent notification",
                summary = "Repeat until released",
                error = "Denied notification access permission",
                isErrorFixable = true,
                icon = ComposeIconInfo.Vector(Icons.Outlined.ClearAll),
            ),
            isExpanded = false,
            isDragging = false,
            isReorderingEnabled = true,
            index = 0,
        )
    }
}

@Preview
@Composable
private fun CollapsedErrorDisabledPreview() {
    KeyMapperTheme {
        ActionListItem(
            model = ActionListItemModel(
                id = "id",
                title = "Dismiss most recent notification",
                summary = "Repeat until released",
                error = "Denied notification access permission",
                isErrorFixable = true,
                icon = ComposeIconInfo.Vector(Icons.Outlined.ClearAll),
                isEnabled = false,
            ),
            isExpanded = false,
            isDragging = false,
            isReorderingEnabled = true,
            index = 0,
        )
    }
}

@Preview
@Composable
private fun CollapsedOneLinePreview() {
    KeyMapperTheme {
        ActionListItem(
            model = ActionListItemModel(
                id = "id",
                title = "Clear all",
                icon = ComposeIconInfo.Vector(Icons.Outlined.ClearAll),
            ),
            isExpanded = false,
            isDragging = false,
            isReorderingEnabled = false,
            index = 0,
        )
    }
}

@PreviewLightDark
@Composable
private fun ExpandedPreview() {
    KeyMapperTheme {
        ActionListItem(
            model = ActionListItemModel(
                id = "id",
                title = "Open magnifier",
                isCustomName = true,
                error = "A Key Mapper keyboard must be enabled!",
                isErrorFixable = true,
                showRepeat = true,
                repeatText = "Repeat 5x after 400ms every 50ms until pressed again",
                showHoldDown = true,
                burstText = null,
                holdDownText = "Hold down for 1000ms",
                icon = ComposeIconInfo.Vector(Icons.Outlined.ClearAll),
            ),
            isExpanded = true,
            isDragging = false,
            isReorderingEnabled = true,
            index = 0,
        )
    }
}

@Preview(widthDp = 300)
@Composable
private fun ExpandedPreviewSmall() {
    KeyMapperTheme {
        ActionListItem(
            model = ActionListItemModel(
                id = "id",
                title = "Open magnifier",
                isCustomName = true,
                error = "A Key Mapper keyboard must be enabled!",
                isErrorFixable = true,
                showRepeat = true,
                repeatText = "Repeat 5x after 400ms every 50ms until pressed again",
                showHoldDown = true,
                burstText = null,
                holdDownText = "Hold down for 1000ms",
                icon = ComposeIconInfo.Vector(Icons.Outlined.ClearAll),
            ),
            isExpanded = true,
            isDragging = false,
            isReorderingEnabled = true,
            index = 0,
        )
    }
}

@Preview
@Composable
private fun ExpandedNoErrorPreview() {
    KeyMapperTheme {
        ActionListItem(
            model = ActionListItemModel(
                id = "id",
                title = "Open magnifier",
                isCustomName = true,
                error = null,
                isErrorFixable = true,
                showRepeat = true,
                repeatText = "Repeat 5x after 400ms every 50ms until pressed again",
                showHoldDown = true,
                burstText = null,
                holdDownText = "Hold down for 1000ms",
                icon = ComposeIconInfo.Vector(Icons.Outlined.ClearAll),
            ),
            isExpanded = true,
            isDragging = false,
            isReorderingEnabled = true,
            index = 0,
        )
    }
}

@Preview
@Composable
private fun DisabledDrawablePreview() {
    val drawable = LocalContext.current.drawable(R.mipmap.ic_launcher_round)

    KeyMapperTheme {
        ActionListItem(
            model = ActionListItemModel(
                id = "id",
                title = "Dismiss most recent notification",
                summary = "Repeat until released",
                isEnabled = false,
                icon = ComposeIconInfo.Drawable(drawable),
            ),
            isExpanded = false,
            isDragging = false,
            isReorderingEnabled = true,
            index = 0,
        )
    }
}
