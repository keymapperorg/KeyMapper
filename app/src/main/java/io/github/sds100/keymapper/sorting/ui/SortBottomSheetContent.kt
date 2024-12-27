package io.github.sds100.keymapper.sorting.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
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
import kotlinx.coroutines.launch

@Composable
fun SortBottomSheet(
    onDismissRequest: () -> Unit,
    viewModel: SortViewModel,
    modifier: Modifier = Modifier,
) {
    val sortFieldOrderList by viewModel.state.collectAsStateWithLifecycle()
    val showHelp by viewModel.showHelp.collectAsStateWithLifecycle()

    SortBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        onApply = viewModel::applySortPriority,
        onCancel = viewModel::restoreState,
        sortFieldOrderList = sortFieldOrderList,
        onMove = viewModel::swapSortPriority,
        onToggle = viewModel::toggleSortOrder,
        canReset = sortFieldOrderList.any { it.order != SortOrder.NONE },
        onReset = viewModel::resetSortPriority,
        showHelp = showHelp,
        onHideHelpClick = { viewModel.setShowHelp(false) },
        onShowHelpClick = { viewModel.setShowHelp(true) },
        onShowExample = viewModel::showExample,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(
    onDismissRequest: () -> Unit,
    sortFieldOrderList: List<SortFieldOrder>,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onToggle: (SortField) -> Unit,
    canReset: Boolean,
    onReset: () -> Unit,
    showHelp: Boolean?,
    onHideHelpClick: () -> Unit,
    onShowHelpClick: () -> Unit,
    onShowExample: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    val coroutineScope = rememberCoroutineScope()
    val dismissSheet: (afterBlock: suspend () -> Unit) -> Unit = { block ->
        coroutineScope.launch {
            block()
            sheetState.hide()
            onDismissRequest()
        }
    }

    ModalBottomSheet(
        modifier = modifier.statusBarsPadding(),
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        // Hide drag handle because other bottom sheets don't have it
        dragHandle = {},
    ) {
        SortBottomSheetContent(
            onCancel = {
                dismissSheet { onCancel() }
            },
            onApply = {
                dismissSheet { onApply() }
            },
            sortFieldOrderList = sortFieldOrderList,
            onMove = onMove,
            onToggle = onToggle,
            canReset = canReset,
            onReset = onReset,
            showHelp = showHelp,
            onHideHelpClick = onHideHelpClick,
            onShowHelpClick = onShowHelpClick,
            onShowExample = onShowExample,
        )
    }
}

@Composable
private fun SortBottomSheetContent(
    onCancel: () -> Unit,
    onApply: () -> Unit,
    sortFieldOrderList: List<SortFieldOrder>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onToggle: (SortField) -> Unit,
    canReset: Boolean,
    onReset: () -> Unit,
    showHelp: Boolean?,
    onHideHelpClick: () -> Unit,
    onShowHelpClick: () -> Unit,
    onShowExample: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var helpExpanded by rememberSaveable { mutableStateOf(false) }
    val scrollableState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.verticalScroll(scrollableState),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            this@Column.AnimatedVisibility(
                visible = showHelp == false,
                enter = slideInHorizontally(),
                exit = slideOutHorizontally { -2 * it },
            ) {
                IconButton(
                    onClick = {
                        onShowHelpClick()
                        helpExpanded = true
                    },
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_help_outline_24),
                        contentDescription = stringResource(R.string.button_help),
                    )
                }
            }

            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(R.string.dialog_message_sort_sort_by),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
            )

            TextButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = onReset,
                enabled = canReset,
            ) {
                Text(stringResource(R.string.reset))
            }
        }

        SortDraggableList(
            modifier = Modifier.heightIn(max = 400.dp),
            sortFieldOrderList = sortFieldOrderList,
            onMove = onMove,
            onSortFieldClick = onToggle,
        )

        AnimatedVisibility(showHelp == true) {
            SortHelp(
                modifier = Modifier.padding(8.dp),
                onHideHelpClick = {
                    onHideHelpClick()
                    helpExpanded = false
                },
                onShowExample = {
                    coroutineScope.launch {
                        scrollableState.animateScrollTo(0)
                        onShowExample()
                    }
                },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            OutlinedButton(
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.neg_cancel))
            }

            Button(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = onApply,
            ) {
                Text(stringResource(R.string.pos_apply))
            }
        }
    }
}

@Composable
private fun SortDraggableList(
    sortFieldOrderList: List<SortFieldOrder>,
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
            items = sortFieldOrderList,
            key = { index, item -> item.field },
        ) { index, item ->
            DraggableItem(
                dragDropState = dragDropState,
                index = index,
            ) { isDragging ->
                SortFieldListItem(
                    index = index + 1,
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
    index: Int,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = index.toString(),
            )
            Text(
                text = stringSortField(sortField),
                style = if (sortOrder == SortOrder.NONE) {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal)
                } else {
                    MaterialTheme.typography.titleMedium
                },
            )
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
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    startDragImmediately = true,
                    onDragStarted = onDragStarted,
                    onDragStopped = onDragStopped,
                ),
        ) {
            Icon(
                modifier = Modifier.align(Alignment.Center),
                imageVector = Icons.Default.DragHandle,
                contentDescription = stringResource(
                    R.string.drag_handle_for,
                    stringSortField(sortField),
                ),
            )
        }
    }
}

@Composable
private fun SortHelp(
    onHideHelpClick: () -> Unit,
    onShowExample: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_help_outline_24),
                    contentDescription = null,
                )

                Text(
                    text = stringResource(R.string.button_help),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Column {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.sorting_drag_and_drop_list_help),
                    textAlign = TextAlign.Justify,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = stringResource(R.string.sorting_drag_and_drop_list_help_example),
                    textAlign = TextAlign.Justify,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = onHideHelpClick,
                    ) {
                        Text(stringResource(R.string.neutral_hide))
                    }
                    TextButton(
                        onClick = onShowExample,
                    ) {
                        Text(stringResource(R.string.show_example))
                    }
                }
            }
        }
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
                sortFieldOrderList = list,
                onMove = { _, _ -> },
                onToggle = {},
                canReset = true,
                onReset = {},
                onHideHelpClick = {},
                showHelp = true,
                onShowHelpClick = {},
                onShowExample = {},
            )
        }
    }
}

@Preview
@Composable
private fun SortBottomSheetPreview() {
    val list = listOf(
        SortFieldOrder(SortField.TRIGGER, SortOrder.NONE),
        SortFieldOrder(SortField.ACTIONS, SortOrder.ASCENDING),
        SortFieldOrder(SortField.CONSTRAINTS, SortOrder.DESCENDING),
        SortFieldOrder(SortField.OPTIONS, SortOrder.NONE),
    )

    var size by remember { mutableIntStateOf(0) }

    KeyMapperTheme {
        Surface {
            SortBottomSheet(
                // Preview hack, breaks if you run it
                modifier = Modifier
                    .offset { IntOffset(0, -size) }
                    .onSizeChanged { size = it.height },
                onDismissRequest = {},
                onApply = {},
                onCancel = {},
                sortFieldOrderList = list,
                onMove = { _, _ -> },
                onToggle = {},
                canReset = true,
                onReset = {},
                showHelp = true,
                onHideHelpClick = {},
                onShowHelpClick = {},
                onShowExample = {},
            )
        }
    }
}
