package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex

/**
 * Drag-and-drop state for reordering items in a plain [Column] rather than a lazy list. Unlike
 * [DragDropState], which compares on-screen bounds, items are swapped one at a time as soon as
 * the dragged item has moved over half of its own height.
 */
@Stable
class ColumnDragDropState internal constructor(
    private val itemCount: () -> Int,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    var draggingIndex by mutableStateOf<Int?>(null)
        private set

    var dragOffset by mutableFloatStateOf(0f)
        private set

    /**
     * The last measured height of whichever item is currently laid out at each index.
     */
    private val itemHeights = mutableStateMapOf<Int, Int>()

    internal fun onSizeChanged(index: Int, height: Int) {
        itemHeights[index] = height
    }

    fun onDragStarted(index: Int) {
        draggingIndex = index
        dragOffset = 0f
    }

    fun onDragStopped() {
        draggingIndex = null
        dragOffset = 0f
    }

    fun onDrag(delta: Float) {
        val currentIndex = draggingIndex ?: return
        val itemHeight = itemHeights[currentIndex] ?: return

        dragOffset += delta

        if (dragOffset > itemHeight / 2f && currentIndex < itemCount() - 1) {
            onMove(currentIndex, currentIndex + 1)
            draggingIndex = currentIndex + 1
            dragOffset -= itemHeight
        } else if (dragOffset < -itemHeight / 2f && currentIndex > 0) {
            onMove(currentIndex, currentIndex - 1)
            draggingIndex = currentIndex - 1
            dragOffset += itemHeight
        }
    }
}

@Composable
fun rememberColumnDragDropState(
    itemCount: Int,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
): ColumnDragDropState {
    val latestItemCount by rememberUpdatedState(itemCount)
    val latestOnMove by rememberUpdatedState(onMove)

    return remember {
        ColumnDragDropState(
            itemCount = { latestItemCount },
            onMove = { fromIndex, toIndex -> latestOnMove(fromIndex, toIndex) },
        )
    }
}

/**
 * Wraps a single item of a [ColumnDragDropState]-driven list. Must be called inside a
 * [LookaheadScope] so the other items can animate into place while one is being dragged.
 *
 * [content] must apply the given `heightModifier` to only the draggable part of the item, not to
 * any extra decoration rendered below it (such as a separator), otherwise the drag distance
 * needed to swap items would be thrown off by that decoration's height.
 */
@Composable
fun LookaheadScope.DraggableColumnItem(
    dragDropState: ColumnDragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(isDragging: Boolean, heightModifier: Modifier) -> Unit,
) {
    val isDragging = dragDropState.draggingIndex == index

    // Only animate the items being displaced by the drag. The dragging item's position is
    // driven directly by the touch via translationY below, so animating its bounds too would
    // fight with that and make it lag behind the finger.
    val placementModifier = if (isDragging) Modifier else Modifier.animateBounds(this)

    Column(
        modifier = modifier
            .then(placementModifier)
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer { translationY = if (isDragging) dragDropState.dragOffset else 0f },
    ) {
        val heightModifier = Modifier.onSizeChanged {
            dragDropState.onSizeChanged(index, it.height)
        }
        content(isDragging, heightModifier)
    }
}
