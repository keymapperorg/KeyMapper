package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import kotlin.math.ceil

/**
 * A [Text] that is only as wide as its longest line. A normal [Text] that wraps takes up the
 * whole max width even if its lines are shorter than that.
 */
@Composable
fun ShrinkWrapText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val textMeasurer = rememberTextMeasurer()

    Text(
        modifier = modifier.layout { measurable, constraints ->
            val result = textMeasurer.measure(
                text = text,
                style = style,
                overflow = overflow,
                maxLines = maxLines,
                constraints = constraints,
            )
            // Round up so a sub-pixel shortfall doesn't make the last word wrap.
            val widest = (0 until result.lineCount).maxOf {
                ceil(result.getLineRight(it) - result.getLineLeft(it)).toInt()
            }
            val placeable = measurable.measure(
                constraints.copy(
                    maxWidth = widest.coerceIn(constraints.minWidth, constraints.maxWidth),
                ),
            )

            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        },
        text = text,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        Column(Modifier.width(150.dp)) {
            ShrinkWrapText(text = "Short")
            ShrinkWrapText(text = "Flashlight is on (very very long text)", maxLines = 2)
        }
    }
}
