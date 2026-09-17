package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme

const val EXPAND_ANIMATION_DURATION = 300

/**
 * The fade and size change share the same duration and easing so the content does not disappear
 * before the card has finished changing shape.
 */
val expandTransition: EnterTransition =
    fadeIn(tween(EXPAND_ANIMATION_DURATION, easing = FastOutSlowInEasing)) +
        expandVertically(
            animationSpec = tween(EXPAND_ANIMATION_DURATION, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top,
        )

val collapseTransition: ExitTransition =
    fadeOut(tween(EXPAND_ANIMATION_DURATION, easing = FastOutSlowInEasing)) +
        shrinkVertically(
            animationSpec = tween(EXPAND_ANIMATION_DURATION, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top,
        )

/**
 * The header content is laid out in a row and is also in a lookahead scope so it can animate
 * its bounds while the card expands.
 */
interface ExpandableDraggableCardHeaderScope :
    RowScope,
    LookaheadScope

private class ExpandableDraggableCardHeaderScopeImpl(
    rowScope: RowScope,
    lookaheadScope: LookaheadScope,
) : ExpandableDraggableCardHeaderScope,
    RowScope by rowScope,
    LookaheadScope by lookaheadScope

/**
 * A card that can be expanded by clicking its header and reordered by dragging its handle.
 *
 * @param key The key of this item in the drag and drop state.
 * @param headerContent The content of the header row. It is always visible and is passed whether
 * the card is expanded.
 * @param expandedContent The content that is only visible when the card is expanded.
 */
@Composable
fun ExpandableDraggableCard(
    modifier: Modifier = Modifier,
    key: Any,
    isExpanded: Boolean,
    isDragging: Boolean,
    isReorderingEnabled: Boolean,
    dragHandleContentDescription: String,
    expandContentDescription: String,
    collapseContentDescription: String,
    isDraggingEnabled: Boolean = true,
    dragDropState: DragDropState? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onExpandedChange: (Boolean) -> Unit = {},
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    headerContent: @Composable ExpandableDraggableCardHeaderScope.(isExpanded: Boolean) -> Unit,
    expandedContent: @Composable () -> Unit,
) {
    val draggableState = rememberDraggableState {
        dragDropState?.onDrag(Offset(0f, it))
    }

    val moveUpLabel = stringResource(R.string.accessibility_action_move_up)
    val moveDownLabel = stringResource(R.string.accessibility_action_move_down)

    val cardColors = CardDefaults.elevatedCardColors(
        containerColor = if (isDragging) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = contentColor,
    )

    ElevatedCard(
        modifier = modifier
            .semantics {
                if (isReorderingEnabled) {
                    customActions = buildList {
                        onMoveUp?.let { action ->
                            add(
                                CustomAccessibilityAction(moveUpLabel) {
                                    action()
                                    true
                                },
                            )
                        }
                        onMoveDown?.let { action ->
                            add(
                                CustomAccessibilityAction(moveDownLabel) {
                                    action()
                                    true
                                },
                            )
                        }
                    }
                }
            },
        colors = cardColors,
    ) {
        LookaheadScope {
            val lookaheadScope = this

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExpandedChange(!isExpanded) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.width(8.dp))

                    if (isReorderingEnabled) {
                        Icon(
                            modifier = Modifier
                                .size(24.dp)
                                .draggable(
                                    state = draggableState,
                                    enabled = isDraggingEnabled,
                                    orientation = Orientation.Vertical,
                                    startDragImmediately = true,
                                    onDragStarted = {
                                        dragDropState?.onDragStart(key)
                                    },
                                    onDragStopped = { dragDropState?.onDragInterrupted() },
                                ),
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = dragHandleContentDescription,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    val headerScope = remember(this, lookaheadScope) {
                        ExpandableDraggableCardHeaderScopeImpl(this, lookaheadScope)
                    }

                    headerScope.headerContent(isExpanded)

                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentSize provides 16.dp,
                    ) {
                        IconButton(onClick = { onExpandedChange(!isExpanded) }) {
                            Icon(
                                imageVector = if (isExpanded) {
                                    Icons.Rounded.KeyboardArrowUp
                                } else {
                                    Icons.Rounded.KeyboardArrowDown
                                },
                                contentDescription = if (isExpanded) {
                                    collapseContentDescription
                                } else {
                                    expandContentDescription
                                },
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Spacer(Modifier.width(4.dp))
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandTransition,
                    exit = collapseTransition,
                ) {
                    expandedContent()
                }
            }
        }
    }
}

@Preview
@Composable
private fun CollapsedPreview() {
    KeyMapperTheme {
        ExpandableDraggableCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            key = "key",
            isExpanded = false,
            isDragging = false,
            isReorderingEnabled = true,
            dragHandleContentDescription = "Drag handle",
            expandContentDescription = "Expand",
            collapseContentDescription = "Collapse",
            headerContent = {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    text = "Header",
                )
            },
            expandedContent = {
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
                    text = "Expanded content",
                )
            },
        )
    }
}

@Preview
@Composable
private fun ExpandedPreview() {
    KeyMapperTheme {
        ExpandableDraggableCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            key = "key",
            isExpanded = true,
            isDragging = false,
            isReorderingEnabled = true,
            dragHandleContentDescription = "Drag handle",
            expandContentDescription = "Expand",
            collapseContentDescription = "Collapse",
            headerContent = {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    text = "Header",
                )
            },
            expandedContent = {
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
                    text = "Expanded content",
                )
            },
        )
    }
}
