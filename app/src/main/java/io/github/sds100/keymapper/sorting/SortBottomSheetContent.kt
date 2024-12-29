package io.github.sds100.keymapper.sorting

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.height
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SortBottomSheet(
    onDismissRequest: () -> Unit,
    viewModel: SortViewModel,
) {
    val sortFieldOrderList by viewModel.sortFieldOrder.collectAsStateWithLifecycle()
    val showHelp by viewModel.showHelp.collectAsStateWithLifecycle()

    SortBottomSheet(
        modifier = Modifier.statusBarsPadding(),
        sortFieldOrderList = sortFieldOrderList,
        showHelp = showHelp,
        onDismissRequest = onDismissRequest,
        onApply = viewModel::applySortPriority,
        onMove = viewModel::swapSortPriority,
        onToggle = viewModel::toggleSortOrder,
        onReset = viewModel::resetSortPriority,
        onHideHelpClick = { viewModel.setShowHelp(false) },
        onShowHelpClick = { viewModel.setShowHelp(true) },
        onShowExample = viewModel::showExample,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    sortFieldOrderList: List<SortFieldOrder>,
    showHelp: Boolean,
    onApply: () -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onToggle: (SortField) -> Unit,
    onReset: () -> Unit,
    onHideHelpClick: () -> Unit,
    onShowHelpClick: () -> Unit,
    onShowExample: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        // Hide drag handle because other bottom sheets don't have it
        dragHandle = {},
    ) {
        SortBottomSheetContent(
            onCancel = {
                coroutineScope.launch {
                    sheetState.hide()
                    onDismissRequest()
                }
            },
            onApply = {
                coroutineScope.launch {
                    onApply()
                    sheetState.hide()
                    onDismissRequest()
                }
            },
            sortFieldOrderList = sortFieldOrderList,
            onMove = onMove,
            onToggle = onToggle,
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
    modifier: Modifier = Modifier,
    sortFieldOrderList: List<SortFieldOrder>,
    showHelp: Boolean,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onToggle: (SortField) -> Unit,
    onReset: () -> Unit,
    onHideHelpClick: () -> Unit,
    onShowHelpClick: () -> Unit,
    onShowExample: () -> Unit,
) {
    var isHelpExpanded by rememberSaveable { mutableStateOf(false) }
    val scrollableState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.verticalScroll(scrollableState),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Use fully qualified name due to quirky overload resolution. The compiler will
            // otherwise tell you to use it in a column or row scope.
            androidx.compose.animation.AnimatedVisibility(
                modifier = Modifier.align(Alignment.CenterStart),
                visible = !showHelp,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                HelpButton {
                    onShowHelpClick()
                    isHelpExpanded = true
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
                enabled = sortFieldOrderList.any { it.order != SortOrder.NONE },
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

        AnimatedVisibility(showHelp) {
            SortHelpCard(
                modifier = Modifier.padding(8.dp),
                onHideHelpClick = {
                    onHideHelpClick()
                    isHelpExpanded = false
                },
                onShowExampleClick = {
                    coroutineScope.launch {
                        scrollableState.animateScrollTo(0)
                        onShowExample()
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(
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
                onClick = onApply,
            ) {
                Text(stringResource(R.string.pos_apply))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HelpButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_help_outline_24),
            contentDescription = stringResource(R.string.button_help),
        )
    }
}

@Composable
private fun SortDraggableList(
    modifier: Modifier = Modifier,
    sortFieldOrderList: List<SortFieldOrder>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onSortFieldClick: (SortField) -> Unit,
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
            key = { _, item -> item.field },
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
                    onDragStarted = { offset ->
                        // Calculate the offset of the item in the list
                        val lazyItem = lazyListState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.index == index } ?: return@SortFieldListItem

                        val initialOffset = lazyItem.offset

                        val finalOffset = offset + Offset(0f, initialOffset.toFloat())

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
            .padding(horizontal = 8.dp),
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
                text = sortFieldText(sortField),
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
                    sortFieldText(sortField),
                ),
            )
        }
    }
}

@Composable
private fun SortHelpCard(
    modifier: Modifier = Modifier,
    onHideHelpClick: () -> Unit,
    onShowExampleClick: () -> Unit,
) {
    Card(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.sorting_drag_and_drop_list_help),
                textAlign = TextAlign.Justify,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.sorting_drag_and_drop_list_help_example),
                textAlign = TextAlign.Justify,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                    onClick = onShowExampleClick,
                ) {
                    Text(stringResource(R.string.show_example))
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
                sortFieldOrderList = list,
                onMove = { _, _ -> },
                onToggle = {},
                onReset = {},
                showHelp = true,
                onHideHelpClick = {},
                onShowHelpClick = {},
                onShowExample = {},
            )
        }
    }
}

@Composable
private fun sortFieldText(sortField: SortField): String {
    return when (sortField) {
        SortField.TRIGGER -> stringResource(R.string.trigger_header)
        SortField.ACTIONS -> stringResource(R.string.action_list_header)
        SortField.CONSTRAINTS -> stringResource(R.string.constraint_list_header)
        SortField.OPTIONS -> stringResource(R.string.option_list_header)
    }
}
