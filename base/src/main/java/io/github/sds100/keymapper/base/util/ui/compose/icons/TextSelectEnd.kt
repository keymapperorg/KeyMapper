package io.github.sds100.keymapper.base.util.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.TextSelectEnd: ImageVector
    get() {
        if (_TextSelectEnd != null) {
            return _TextSelectEnd!!
        }
        _TextSelectEnd = ImageVector.Builder(
            name = "TextSelectEnd",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(440f, 200f)
                lineTo(440f, 120f)
                lineTo(520f, 120f)
                lineTo(520f, 200f)
                lineTo(440f, 200f)
                close()
                moveTo(440f, 840f)
                lineTo(440f, 760f)
                lineTo(520f, 760f)
                lineTo(520f, 840f)
                lineTo(440f, 840f)
                close()
                moveTo(280f, 200f)
                lineTo(280f, 120f)
                lineTo(360f, 120f)
                lineTo(360f, 200f)
                lineTo(280f, 200f)
                close()
                moveTo(280f, 840f)
                lineTo(280f, 760f)
                lineTo(360f, 760f)
                lineTo(360f, 840f)
                lineTo(280f, 840f)
                close()
                moveTo(120f, 200f)
                lineTo(120f, 120f)
                lineTo(200f, 120f)
                lineTo(200f, 200f)
                lineTo(120f, 200f)
                close()
                moveTo(120f, 360f)
                lineTo(120f, 280f)
                lineTo(200f, 280f)
                lineTo(200f, 360f)
                lineTo(120f, 360f)
                close()
                moveTo(120f, 520f)
                lineTo(120f, 440f)
                lineTo(200f, 440f)
                lineTo(200f, 520f)
                lineTo(120f, 520f)
                close()
                moveTo(120f, 680f)
                lineTo(120f, 600f)
                lineTo(200f, 600f)
                lineTo(200f, 680f)
                lineTo(120f, 680f)
                close()
                moveTo(120f, 840f)
                lineTo(120f, 760f)
                lineTo(200f, 760f)
                lineTo(200f, 840f)
                lineTo(120f, 840f)
                close()
                moveTo(600f, 840f)
                lineTo(600f, 760f)
                lineTo(680f, 760f)
                lineTo(680f, 200f)
                lineTo(600f, 200f)
                lineTo(600f, 120f)
                lineTo(840f, 120f)
                lineTo(840f, 200f)
                lineTo(760f, 200f)
                lineTo(760f, 760f)
                lineTo(840f, 760f)
                lineTo(840f, 840f)
                lineTo(600f, 840f)
                close()
            }
        }.build()

        return _TextSelectEnd!!
    }

@Suppress("ObjectPropertyName")
private var _TextSelectEnd: ImageVector? = null
