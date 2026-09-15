package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * The distance past the edge of the list at which auto scrolling reaches its maximum speed.
 */
private val autoScrollEdgeDistance = 64.dp

/**
 * Maximum auto scroll speed per second.
 */
private val autoScrollMaxSpeed = 1000.dp

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    /**
     * The keys of the items that can be dragged, in their current order. Other items in the list,
     * such as headers and footers, can not be dragged and items can not be dropped on them.
     */
    keys: List<Any>,
    /**
     * Called once when the drag ends with the indices in [keys]. While dragging, the order is
     * only changed locally, so use [DragDropState.ordered] to display the items.
     */
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onStart: () -> Unit = {},
    onEnd: () -> Unit = {},
): DragDropState {
    val scope = rememberCoroutineScope()
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnEnd by rememberUpdatedState(onEnd)

    val state = remember(lazyListState) {
        DragDropState(
            state = lazyListState,
            scope = scope,
            onStart = { currentOnStart() },
            onMove = { fromIndex, toIndex -> currentOnMove(fromIndex, toIndex) },
            onEnd = { currentOnEnd() },
        )
    }

    SideEffect {
        state.updateKeys(keys)
    }

    val density = LocalDensity.current
    val edgeDistancePx = with(density) { autoScrollEdgeDistance.toPx() }
    val maxSpeedPx = with(density) { autoScrollMaxSpeed.toPx() }

    LaunchedEffect(state, edgeDistancePx, maxSpeedPx) {
        snapshotFlow { state.draggingItemKey != null }.collectLatest { isDragging ->
            if (!isDragging) {
                return@collectLatest
            }

            var lastFrameNanos = withFrameNanos { it }

            while (true) {
                val frameNanos = withFrameNanos { it }
                val seconds = (frameNanos - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = frameNanos

                val overflow = state.draggingItemOverflow()
                if (overflow != 0f) {
                    val fraction = (overflow / edgeDistancePx).coerceIn(-1f, 1f)
                    lazyListState.scrollBy(fraction * maxSpeedPx * seconds)
                }

                // Check again after scrolling because other items move under the dragged item.
                state.moveToTarget()
            }
        }
    }

    return state
}

/**
 * Originally based on the official demo for drag and drop at https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation/integration-tests/foundation-demos/src/main/java/androidx/compose/foundation/demos/LazyColumnDragAndDropDemo.kt
 *
 * Items are tracked by their key rather than their index so the list can contain other items,
 * such as headers and footers. The new order is kept locally until the drag ends so the list
 * does not depend on the new order propagating back from a ViewModel while dragging.
 */
class DragDropState internal constructor(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val onStart: () -> Unit,
    private val onMove: (Int, Int) -> Unit,
    private val onEnd: () -> Unit,
) {
    var draggingItemKey by mutableStateOf<Any?>(null)
        private set

    private var keys: List<Any> = emptyList()

    /**
     * The order of the keys while dragging, and after the drag ends until the new order is
     * received in [updateKeys].
     */
    private var pendingOrder by mutableStateOf<List<Any>?>(null)

    private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)
    internal val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
        } ?: 0f

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = draggingItemKey?.let { key ->
            state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
        }

    /**
     * The layout info when the order was last changed. Do not change the order again until the
     * list has been laid out with the new order, otherwise the item can move back and forth.
     */
    private var layoutInfoAtLastMove: LazyListLayoutInfo? = null

    internal var previousKeyOfDraggedItem by mutableStateOf<Any?>(null)
        private set
    internal var previousItemOffset = Animatable(0f)
        private set

    /**
     * Sort the items in the order they should be displayed while dragging.
     */
    fun <T> ordered(items: List<T>, key: (T) -> Any): List<T> {
        val order = pendingOrder ?: return items
        val positions = order.withIndex().associate { it.value to it.index }
        return items.sortedBy { positions[key(it)] ?: Int.MAX_VALUE }
    }

    internal fun updateKeys(newKeys: List<Any>) {
        if (newKeys == keys) {
            return
        }

        keys = newKeys

        // The moved list has been received so the local order is no longer needed.
        if (draggingItemKey == null) {
            pendingOrder = null
        }
    }

    /**
     * Start dragging the item with this key. Use this when dragging with a drag handle.
     */
    fun onDragStart(key: Any) {
        if (key !in keys) {
            return
        }

        val item = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        startDragging(item)
    }

    /**
     * Start dragging the item at this offset in the list.
     */
    fun onDragStart(offset: Offset) {
        val item = state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.key in keys && offset.y.toInt() in item.offset..item.offsetEnd
        } ?: return

        startDragging(item)
    }

    private fun startDragging(item: LazyListItemInfo) {
        pendingOrder = ordered(keys) { it }
        draggingItemKey = item.key
        draggingItemInitialOffset = item.offset
        draggingItemDraggedDelta = 0f
        layoutInfoAtLastMove = null

        onStart.invoke()
    }

    fun onDragInterrupted() {
        val key = draggingItemKey

        if (key != null) {
            previousKeyOfDraggedItem = key
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
                previousKeyOfDraggedItem = null
            }

            val fromIndex = keys.indexOf(key)
            val toIndex = ordered(keys) { it }.indexOf(key)

            if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                // Keep the local order until the new keys are received.
                onMove.invoke(fromIndex, toIndex)
            } else {
                pendingOrder = null
            }
        }

        draggingItemDraggedDelta = 0f
        draggingItemKey = null
        draggingItemInitialOffset = 0
        layoutInfoAtLastMove = null

        onEnd.invoke()
    }

    fun onDrag(offset: Offset) {
        draggingItemDraggedDelta += offset.y
        moveToTarget()
    }

    /**
     * Move the dragged item to the position of the item underneath its middle.
     */
    internal fun moveToTarget() {
        val draggingItem = draggingItemLayoutInfo ?: return
        val layoutInfo = state.layoutInfo
        val order = pendingOrder ?: return

        if (layoutInfo === layoutInfoAtLastMove) {
            return
        }

        val startOffset = draggingItem.offset + draggingItemOffset
        val middleOffset = startOffset + draggingItem.size / 2f

        val targetItem = layoutInfo.visibleItemsInfo.find { item ->
            item.key != draggingItem.key &&
                item.key in keys &&
                middleOffset.toInt() in item.offset..item.offsetEnd
        } ?: return

        // Only move once the middle of the dragged item has passed the middle of the target.
        // Otherwise items of different heights swap back and forth.
        val targetMiddle = targetItem.offset + targetItem.size / 2f
        val hasPassedTarget = if (targetItem.offset > draggingItem.offset) {
            middleOffset > targetMiddle
        } else {
            middleOffset < targetMiddle
        }

        if (!hasPassedTarget) {
            return
        }

        val fromIndex = order.indexOf(draggingItem.key)
        val toIndex = order.indexOf(targetItem.key)

        if (fromIndex == -1 || toIndex == -1) {
            return
        }

        // Where the dragged item will be laid out after moving if the other items stay in place.
        val newOffset = if (targetItem.offset > draggingItem.offset) {
            targetItem.offsetEnd - draggingItem.size
        } else {
            targetItem.offset
        }
        val minOffset = layoutInfo.viewportStartOffset
        val maxOffset =
            (layoutInfo.viewportEndOffset - draggingItem.size).coerceAtLeast(minOffset)

        if (newOffset !in minOffset..maxOffset) {
            // Moving a small item past a large item can move it out of the list, which stops
            // it being laid out so the drag can not continue. Scroll so it stays in the list.
            // The dragged item will be at the target's index after moving.
            state.requestScrollToItem(targetItem.index, -newOffset.coerceIn(minOffset, maxOffset))
        } else if (draggingItem.index == state.firstVisibleItemIndex ||
            targetItem.index == state.firstVisibleItemIndex
        ) {
            // The list keeps the first visible item in place when the order changes, which
            // scrolls the list when the first visible item is moved. Keep the scroll position.
            state.requestScrollToItem(
                state.firstVisibleItemIndex,
                state.firstVisibleItemScrollOffset,
            )
        }

        pendingOrder = order.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        layoutInfoAtLastMove = layoutInfo
    }

    /**
     * How far the dragged item is past the edge of the list in the direction it is being dragged.
     * Positive when past the end and negative when past the start.
     */
    internal fun draggingItemOverflow(): Float {
        val draggingItem = draggingItemLayoutInfo ?: return 0f
        val layoutInfo = state.layoutInfo

        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size

        return when {
            draggingItemDraggedDelta > 0 ->
                (endOffset - layoutInfo.viewportEndOffset).coerceAtLeast(0f)

            draggingItemDraggedDelta < 0 ->
                (startOffset - layoutInfo.viewportStartOffset).coerceAtMost(0f)

            else -> 0f
        }
    }

    private val LazyListItemInfo.offsetEnd: Int
        get() = this.offset + this.size
}

fun Modifier.dragContainer(dragDropState: DragDropState): Modifier =
    this.pointerInput(dragDropState) {
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
