package io.github.sds100.keymapper.sorting.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.compose.draggable.DraggableItem
import io.github.sds100.keymapper.compose.draggable.rememberDragDropState
import io.github.sds100.keymapper.sorting.SortField
import io.github.sds100.keymapper.sorting.SortFieldOrder
import io.github.sds100.keymapper.sorting.SortOrder
import io.github.sds100.keymapper.sorting.SortViewModel
import kotlinx.coroutines.CoroutineScope

@Composable
fun SortBottomSheetContent(
    onExit: () -> Unit,
    viewModel: SortViewModel,
) {
    val list by viewModel.state.collectAsStateWithLifecycle()

    SortBottomSheetContent(
        onApply = {
            viewModel.applySortPriority()
            onExit()
        },
        onCancel = {
            viewModel.restoreState()
            onExit()
        },
        list = list,
        onMove = { fromIndex, toIndex ->
            viewModel.swapSortPriority(fromIndex, toIndex)
        },
        onToggle = { field ->
            viewModel.toggleSortOrder(field)
        },
    )
}

@Composable
private fun SortBottomSheetContent(
    onCancel: () -> Unit,
    onApply: () -> Unit,
    list: List<SortFieldOrder>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onToggle: (SortField) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(
                    text = stringResource(R.string.neg_cancel),
                )
            }
            Text(
                text = stringResource(R.string.dialog_message_sort_sort_by),
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(
                onClick = onApply,
            ) {
                Text(
                    text = stringResource(R.string.pos_apply),
                )
            }
        }

        SortDraggableList(
            list = list,
            onMove = onMove,
            onSortFieldClick = onToggle,
        )
    }
}

@Composable
private fun SortDraggableList(
    list: List<SortFieldOrder>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onSortFieldClick: (SortField) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val dragDropState = rememberDragDropState(
        lazyListState = lazyListState,
        onMove = onMove,
    )

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
    ) {
        itemsIndexed(
            items = list,
            key = { index, item -> item.field },
        ) { index, item ->
            DraggableItem(
                dragDropState = dragDropState,
                index = index,
            ) { isDragging ->
                SortFieldListItem(
                    sortField = item.field,
                    sortOrder = item.order,
                    onToggle = { onSortFieldClick(item.field) },
                    isDragging = isDragging,
                    onDrag = { dragDropState.onDrag(it) },
                    onDragStarted = {
                        // Calculate the offset of the item in the list
                        val lazyItem = lazyListState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.index == index } ?: return@SortFieldListItem

                        val initialOffset = lazyItem.offset

                        val finalOffset = it + Offset(0f, initialOffset.toFloat())

                        dragDropState.onDragStart(finalOffset)
                    },
                    onDragStopped = { dragDropState.onDragInterrupted() },
                )
            }
        }
    }
}

@Composable
private fun SortFieldListItem(
    sortField: SortField,
    sortOrder: SortOrder,
    onToggle: () -> Unit,
    isDragging: Boolean,
    onDrag: (Offset) -> Unit,
    onDragStarted: suspend CoroutineScope.(Offset) -> Unit,
    onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val draggableState = rememberDraggableState { onDrag(Offset(0f, it)) }
    val draggableColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onToggle() }
            .drawBehind {
                if (isDragging) {
                    drawRect(draggableColor)
                }
            }
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedContent(
                targetState = sortOrder,
                transitionSpec = {
                    when (targetState) {
                        SortOrder.ASCENDING ->
                            slideInVertically { it } togetherWith slideOutVertically { -it }

                        SortOrder.DESCENDING ->
                            slideInVertically { -it } togetherWith slideOutVertically { it }

                        SortOrder.NONE ->
                            fadeIn() togetherWith fadeOut()
                    }
                },
                label = "$sortField Sort Order",
            ) { sortOrder ->
                if (sortOrder == SortOrder.NONE) {
                    Spacer(Modifier.size(24.dp))
                    return@AnimatedContent
                }

                val imageVector = when (sortOrder) {
                    SortOrder.NONE -> return@AnimatedContent
                    SortOrder.ASCENDING -> Icons.Default.ArrowUpward
                    SortOrder.DESCENDING -> Icons.Default.ArrowDownward
                }

                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                )
            }
            Text(
                text = stringSortField(sortField),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Icon(
            modifier = Modifier.draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                startDragImmediately = true,
                onDragStarted = onDragStarted,
                onDragStopped = onDragStopped,
            ),
            imageVector = Icons.Default.DragHandle,
            contentDescription = null,
        )
    }
}

@Preview
@Composable
private fun SortBottomSheetContentPreview() {
    val list = listOf(
        SortFieldOrder(SortField.TRIGGER, SortOrder.NONE),
        SortFieldOrder(SortField.ACTIONS, SortOrder.ASCENDING),
        SortFieldOrder(SortField.CONSTRAINTS, SortOrder.DESCENDING),
        SortFieldOrder(SortField.OPTIONS, SortOrder.NONE),
    )

    KeyMapperTheme {
        Surface {
            SortBottomSheetContent(
                onApply = {},
                onCancel = {},
                list = list,
                onMove = { _, _ -> },
                onToggle = {},
            )
        }
    }
}
