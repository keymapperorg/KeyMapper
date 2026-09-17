package io.github.sds100.keymapper.base.constraints

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.DragDropState
import io.github.sds100.keymapper.base.utils.ui.compose.EXPAND_ANIMATION_DURATION
import io.github.sds100.keymapper.base.utils.ui.compose.ExpandableDraggableCard
import io.github.sds100.keymapper.base.utils.ui.compose.horizontalFadingEdges

/**
 * The description and error fade out in the first half of the animation so they are invisible
 * before the shrink clips them. When expanding, the space is made first and the text fades in
 * during the second half.
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
fun ConstraintGroupItem(
    modifier: Modifier = Modifier,
    model: ConstraintGroupListItemModel,
    isExpanded: Boolean,
    isDraggingEnabled: Boolean = false,
    isDragging: Boolean = false,
    isReorderingEnabled: Boolean = false,
    dragDropState: DragDropState? = null,
    onExpandedChange: (Boolean) -> Unit = {},
    onSelectMode: (ConstraintMode) -> Unit = {},
    onAddConstraintClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onRemoveConstraintClick: (String) -> Unit = {},
    onFixConstraintClick: (String) -> Unit = {},
    onNotClick: (String) -> Unit = {},
    onMoveConstraint: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    val countBasedTitle = when {
        model.constraints.size == 1 -> stringResource(R.string.constraint_group_title_single)

        model.mode == ConstraintMode.AND -> pluralStringResource(
            R.plurals.constraint_group_title_and,
            model.constraints.size,
            model.constraints.size,
        )

        else -> pluralStringResource(
            R.plurals.constraint_group_title_or,
            model.constraints.size,
            model.constraints.size,
        )
    }

    val title = model.name ?: countBasedTitle

    ExpandableDraggableCard(
        modifier = modifier,
        key = model.uid,
        isExpanded = isExpanded,
        isDragging = isDragging,
        isReorderingEnabled = isReorderingEnabled,
        isDraggingEnabled = isDraggingEnabled,
        dragDropState = dragDropState,
        dragHandleContentDescription = stringResource(R.string.drag_handle_for, title),
        expandContentDescription = stringResource(R.string.constraint_group_expand),
        collapseContentDescription = stringResource(R.string.constraint_group_collapse),
        onExpandedChange = onExpandedChange,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        headerContent = { expanded ->
            HeaderText(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                title = if (isExpanded) {
                    model.name ?: countBasedTitle
                } else {
                    model.name
                        ?: model.description
                },
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
                onSelectMode = onSelectMode,
                onAddConstraintClick = onAddConstraintClick,
                onDeleteClick = onDeleteClick,
                onRemoveConstraintClick = onRemoveConstraintClick,
                onFixConstraintClick = onFixConstraintClick,
                onNotClick = onNotClick,
                onMoveConstraint = onMoveConstraint,
            )
        },
    )
}

@Composable
private fun HeaderText(
    modifier: Modifier = Modifier,
    title: String,
    model: ConstraintGroupListItemModel,
    isExpanded: Boolean,
    onRenameClick: () -> Unit,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier
                    .weight(1f, fill = false),
                text = title,
                style = MaterialTheme.typography.bodyMedium,
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
            if (model.error != null) {
                Text(
                    text = model.error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ExpandedContent(
    modifier: Modifier = Modifier,
    model: ConstraintGroupListItemModel,
    onSelectMode: (ConstraintMode) -> Unit,
    onAddConstraintClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRemoveConstraintClick: (String) -> Unit,
    onFixConstraintClick: (String) -> Unit,
    onNotClick: (String) -> Unit,
    onMoveConstraint: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    Column(modifier) {
        Text(
            modifier = Modifier.padding(end = 8.dp),
            text = when {
                model.constraints.size == 1 ->
                    stringResource(R.string.constraint_group_explanation_single)

                model.mode == ConstraintMode.AND ->
                    stringResource(R.string.constraint_group_explanation_and)

                else -> stringResource(R.string.constraint_group_explanation_or)
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.size(8.dp))

        GroupConstraintList(
            modifier = Modifier.padding(end = 8.dp),
            constraints = model.constraints,
            mode = model.mode,
            onRemoveConstraintClick = onRemoveConstraintClick,
            onFixConstraintClick = onFixConstraintClick,
            onNotClick = onNotClick,
            onMoveConstraint = onMoveConstraint,
        )

        Spacer(Modifier.size(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConstraintModeButtons(
                modifier = Modifier.width(160.dp),
                mode = model.mode,
                onSelectMode = onSelectMode,
                isEnabled = model.constraints.size > 1,
            )

            Spacer(Modifier.weight(1f))

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
                    IconButton(onClick = onAddConstraintClick) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(
                                R.string.constraint_group_add_constraint,
                            ),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.constraint_group_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The constraints in a group. These are not in a lazy list so they are reordered by swapping
 * the dragged constraint with its neighbour once it has been dragged over half of its height.
 */
@Composable
private fun GroupConstraintList(
    modifier: Modifier = Modifier,
    constraints: List<ConstraintListItemModel>,
    mode: ConstraintMode,
    onRemoveConstraintClick: (String) -> Unit,
    onFixConstraintClick: (String) -> Unit,
    onNotClick: (String) -> Unit,
    onMoveConstraint: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val itemHeights = remember { mutableStateMapOf<String, Int>() }

    val linkText = when (mode) {
        ConstraintMode.AND -> stringResource(R.string.constraint_mode_and)
        ConstraintMode.OR -> stringResource(R.string.constraint_mode_or)
    }

    Column(modifier = modifier) {
        constraints.forEachIndexed { index, constraint ->
            key(constraint.id) {
                val isDragging = draggingIndex == index

                val draggableState = rememberDraggableState { delta ->
                    val currentIndex = draggingIndex ?: return@rememberDraggableState
                    val itemHeight = constraints.getOrNull(currentIndex)
                        ?.let { itemHeights[it.id] }
                        ?: return@rememberDraggableState

                    dragOffset += delta

                    if (dragOffset > itemHeight / 2f && currentIndex < constraints.lastIndex) {
                        onMoveConstraint(currentIndex, currentIndex + 1)
                        draggingIndex = currentIndex + 1
                        dragOffset -= itemHeight
                    } else if (dragOffset < -itemHeight / 2f && currentIndex > 0) {
                        onMoveConstraint(currentIndex, currentIndex - 1)
                        draggingIndex = currentIndex - 1
                        dragOffset += itemHeight
                    }
                }

                Column(
                    modifier = Modifier
                        .onSizeChanged { itemHeights[constraint.id] = it.height }
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f },
                ) {
                    ConstraintListItem(
                        modifier = Modifier.fillMaxWidth(),
                        model = constraint,
                        isReorderingEnabled = constraints.size > 1,
                        isDragging = isDragging,
                        dragHandleModifier = Modifier.draggable(
                            state = draggableState,
                            orientation = Orientation.Vertical,
                            startDragImmediately = true,
                            onDragStarted = {
                                draggingIndex = index
                                dragOffset = 0f
                            },
                            onDragStopped = {
                                draggingIndex = null
                                dragOffset = 0f
                            },
                        ),
                        onRemoveClick = { onRemoveConstraintClick(constraint.id) },
                        onFixClick = { onFixConstraintClick(constraint.id) },
                        onNotClick = { onNotClick(constraint.id) },
                        onMoveUp = if (index > 0) {
                            { onMoveConstraint(index, index - 1) }
                        } else {
                            null
                        },
                        onMoveDown = if (index < constraints.lastIndex) {
                            { onMoveConstraint(index, index + 1) }
                        } else {
                            null
                        },
                    )

                    if (index < constraints.lastIndex) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            text = linkText,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

private val previewConstraints = listOf(
    ConstraintListItemModel(
        id = "1",
        icon = ComposeIconInfo.Vector(Icons.Outlined.FlashlightOn),
        text = "Flashlight is on",
        isNot = true,
        error = "Flashlight not found",
        isErrorFixable = true,
    ),
    ConstraintListItemModel(
        id = "2",
        icon = ComposeIconInfo.Vector(Icons.Outlined.Wifi),
        text = "Wi-Fi is on",
    ),
)

@PreviewLightDark
@Preview(widthDp = 300)
@Composable
private fun AndExpandedPreview() {
    KeyMapperTheme {
        ConstraintGroupItem(
            model = ConstraintGroupListItemModel(
                uid = "group",
                mode = ConstraintMode.AND,
                constraints = previewConstraints,
                description = "Flashlight is not on AND Wi-Fi is on",
            ),
            isExpanded = true,
            isReorderingEnabled = true,
        )
    }
}

@Preview
@Composable
private fun OrExpandedPreview() {
    KeyMapperTheme {
        ConstraintGroupItem(
            model = ConstraintGroupListItemModel(
                uid = "group",
                mode = ConstraintMode.OR,
                constraints = previewConstraints.map { it.copy(error = null, isNot = false) },
                description = "Flashlight is on OR Wi-Fi is on",
            ),
            isExpanded = true,
        )
    }
}

@Preview
@Composable
private fun SingleExpandedPreview() {
    KeyMapperTheme {
        ConstraintGroupItem(
            model = ConstraintGroupListItemModel(
                uid = "group",
                mode = ConstraintMode.AND,
                constraints = listOf(previewConstraints[1]),
                description = "Wi-Fi is on",
            ),
            isExpanded = true,
        )
    }
}

@PreviewLightDark
@Preview(widthDp = 300)
@Composable
private fun CollapsedPreview() {
    KeyMapperTheme {
        ConstraintGroupItem(
            model = ConstraintGroupListItemModel(
                uid = "group",
                mode = ConstraintMode.AND,
                constraints = previewConstraints,
                description = "Flashlight is not on AND Wi-Fi is on",
            ),
            isExpanded = false,
            isReorderingEnabled = true,
        )
    }
}

@Preview
@Composable
private fun CollapsedErrorPreview() {
    KeyMapperTheme {
        ConstraintGroupItem(
            model = ConstraintGroupListItemModel(
                uid = "group",
                mode = ConstraintMode.AND,
                constraints = previewConstraints,
                description = "Flashlight is not on AND Wi-Fi is on",
            ),
            isExpanded = false,
            isReorderingEnabled = true,
        )
    }
}
