package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    /**
     * The same key that is used for this item in the lazy list.
     */
    key: Any,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(isDragging: Boolean) -> Unit,
) {
    val dragging = key == dragDropState.draggingItemKey

    val draggingModifier = if (dragging) {
        Modifier
            .zIndex(1f)
            .graphicsLayer {
                translationY = dragDropState.draggingItemOffset
            }
    } else if (key == dragDropState.previousKeyOfDraggedItem) {
        Modifier
            .zIndex(1f)
            .graphicsLayer {
                translationY = dragDropState.previousItemOffset.value
            }
    } else if (dragDropState.draggingItemKey != null ||
        dragDropState.previousKeyOfDraggedItem != null
    ) {
        // Only animate placement while a drag/drop is actually reflowing the list. Applying
        // this unconditionally makes every row "jiggle" into place whenever the list is laid
        // out for unrelated reasons, e.g. the pager swiping this page into view.
        Modifier.animateItem(
            fadeInSpec = null,
            fadeOutSpec = null,
        )
    } else {
        Modifier
    }
    Box(modifier.then(draggingModifier)) {
        content(dragging)
    }
}
