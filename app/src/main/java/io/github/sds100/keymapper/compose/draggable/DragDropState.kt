package io.github.sds100.keymapper.compose.draggable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    /**
     * Ignore the last N items in the list. Do not allow dragging and dropping these items or
     * placing other items in these positions.
     */
    ignoreLastItems: Int = 0,
    onMove: (Int, Int) -> Unit,
    onStart: () -> Unit = {},
    onEnd: () -> Unit = {},
): DragDropState {
    val scope = rememberCoroutineScope()
    val state = remember(lazyListState) {
        DragDropState(
            state = lazyListState,
            ignoreLastItems = ignoreLastItems,
            onStart = onStart,
            onMove = onMove,
            onEnd = onEnd,
            scope = scope,
        )
    }

    LaunchedEffect(state) {
        while (true) {
            val diff = state.scrollChannel.receive()
            lazyListState.scrollBy(diff)
        }
    }

    return state
}

/**
 * This is copied from an official demo for drag and drop at https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation/integration-tests/foundation-demos/src/main/java/androidx/compose/foundation/demos/LazyColumnDragAndDropDemo.kt
 */
class DragDropState internal constructor(
    private val state: LazyListState,
    private val ignoreLastItems: Int,
    private val scope: CoroutineScope,
    private val onStart: () -> Unit,
    private val onMove: (Int, Int) -> Unit,
    private val onEnd: () -> Unit,
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    internal val scrollChannel = Channel<Float>()

    private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)
    internal val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
        } ?: 0f

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = state.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == draggingItemIndex }

    internal var previousIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set
    internal var previousItemOffset = Animatable(0f)
        private set

    fun onDragStart(index: Int, offset: Offset) {
        Timber.e("ON DRAG START $index")
        // Calculate the offset of the item in the list
        val lazyItem = state.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == index }
            ?: return

        val initialOffset = lazyItem.offset

        val finalOffset = offset + Offset(0f, initialOffset.toFloat())

        onDragStart(finalOffset)
    }

    fun onDragStart(offset: Offset) {
        // check if the touch position is on drag handle
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                item.index < state.layoutInfo.totalItemsCount - ignoreLastItems &&
                    offset.y.toInt() in item.offset..(item.offset + item.size)
            }?.also {
                draggingItemIndex = it.index
                draggingItemInitialOffset = it.offset
            }

        onStart.invoke()
    }

    fun onDragInterrupted() {
        if (draggingItemIndex != null) {
            previousIndexOfDraggedItem = draggingItemIndex
            val startOffset = draggingItemOffset
            scope.launch {
                previousItemOffset.snapTo(startOffset)
                previousItemOffset.animateTo(
                    0f,
                    spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = 1f,
                    ),
                )
                previousIndexOfDraggedItem = null
            }
        }
        draggingItemDraggedDelta = 0f
        draggingItemIndex = null
        draggingItemInitialOffset = 0

        onEnd.invoke()
    }

    fun onDrag(offset: Offset) {
        draggingItemDraggedDelta += offset.y

        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val middleOffset = startOffset + (endOffset - startOffset) / 2f

        val targetItem = state.layoutInfo.visibleItemsInfo.find { item ->
            middleOffset.toInt() in item.offset..item.offsetEnd &&
                draggingItem.index != item.index
        }
        val itemCount = state.layoutInfo.totalItemsCount

        if (targetItem != null) {
            val scrollToIndex = if (targetItem.index == state.firstVisibleItemIndex) {
                draggingItem.index
            } else if (draggingItem.index == state.firstVisibleItemIndex) {
                targetItem.index
            } else {
                null
            }

            if (draggingItem.index < itemCount - ignoreLastItems && targetItem.index < itemCount - ignoreLastItems) {
                if (scrollToIndex != null) {
                    scope.launch {
                        // this is needed to neutralize automatic keeping the first item first.
                        state.scrollToItem(scrollToIndex, state.firstVisibleItemScrollOffset)
                        onMove.invoke(draggingItem.index, targetItem.index)
                    }
                } else {
                    onMove.invoke(draggingItem.index, targetItem.index)
                }
                draggingItemIndex = targetItem.index
            }
        } else {
            val overscroll = when {
                draggingItemDraggedDelta > 0 ->
                    (endOffset - state.layoutInfo.viewportEndOffset).coerceAtLeast(
                        0f,
                    )

                draggingItemDraggedDelta < 0 ->
                    (startOffset - state.layoutInfo.viewportStartOffset).coerceAtMost(0f)

                else -> 0f
            }
            if (overscroll != 0f) {
                scrollChannel.trySend(overscroll)
            }
        }
    }

    private val LazyListItemInfo.offsetEnd: Int
        get() = this.offset + this.size
}

fun Modifier.dragContainer(dragDropState: DragDropState): Modifier = this.pointerInput(dragDropState) {
    detectDragGesturesAfterLongPress(
        onDrag = { change, offset ->
            change.consume()
            dragDropState.onDrag(offset = offset)
        },
        onDragStart = { offset -> dragDropState.onDragStart(offset) },
        onDragEnd = { dragDropState.onDragInterrupted() },
        onDragCancel = { dragDropState.onDragInterrupted() },
    )
}
