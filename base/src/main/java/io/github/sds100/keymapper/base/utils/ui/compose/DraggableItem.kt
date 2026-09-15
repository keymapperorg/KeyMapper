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
    } else {
        Modifier.animateItem(
            fadeInSpec = null,
            fadeOutSpec = null,
        )
    }
    Box(modifier.then(draggingModifier)) {
        content(dragging)
    }
}
