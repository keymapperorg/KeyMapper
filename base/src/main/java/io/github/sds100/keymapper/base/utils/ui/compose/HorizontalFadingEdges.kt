package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * The width of the shadow drawn over the start/end of a horizontally scrollable row to
 * indicate that it can be scrolled.
 */
private val ScrollFadeEdgeWidth = 48.dp

/**
 * Draws a shadow over the start and/or end of the content to indicate it is horizontally
 * scrollable in that direction. The shadow on each side is only drawn while it is possible to
 * scroll further that way.
 */
fun Modifier.horizontalFadingEdges(scrollState: ScrollState): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()

        val fadeWidthPx = ScrollFadeEdgeWidth.toPx()

        if (scrollState.canScrollBackward) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = 0f,
                    endX = fadeWidthPx,
                ),
                blendMode = BlendMode.DstIn,
            )
        }

        if (scrollState.canScrollForward) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = size.width - fadeWidthPx,
                    endX = size.width,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }
