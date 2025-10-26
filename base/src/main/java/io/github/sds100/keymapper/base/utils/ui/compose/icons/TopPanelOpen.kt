package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.TopPanelOpen: ImageVector
    get() {
        if (_TopPanelOpen != null) {
            return _TopPanelOpen!!
        }
        _TopPanelOpen =
            ImageVector
                .Builder(
                    name = "TopPanelOpen",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 960f,
                    viewportHeight = 960f,
                ).apply {
                    path(fill = SolidColor(Color(0xFF000000))) {
                        moveTo(480f, 660f)
                        lineTo(640f, 500f)
                        lineTo(320f, 500f)
                        lineTo(480f, 660f)
                        close()
                        moveTo(200f, 840f)
                        quadTo(167f, 840f, 143.5f, 816.5f)
                        quadTo(120f, 793f, 120f, 760f)
                        lineTo(120f, 200f)
                        quadTo(120f, 167f, 143.5f, 143.5f)
                        quadTo(167f, 120f, 200f, 120f)
                        lineTo(760f, 120f)
                        quadTo(793f, 120f, 816.5f, 143.5f)
                        quadTo(840f, 167f, 840f, 200f)
                        lineTo(840f, 760f)
                        quadTo(840f, 793f, 816.5f, 816.5f)
                        quadTo(793f, 840f, 760f, 840f)
                        lineTo(200f, 840f)
                        close()
                        moveTo(760f, 320f)
                        lineTo(760f, 200f)
                        quadTo(760f, 200f, 760f, 200f)
                        quadTo(760f, 200f, 760f, 200f)
                        lineTo(200f, 200f)
                        quadTo(200f, 200f, 200f, 200f)
                        quadTo(200f, 200f, 200f, 200f)
                        lineTo(200f, 320f)
                        lineTo(760f, 320f)
                        close()
                        moveTo(200f, 400f)
                        lineTo(200f, 760f)
                        quadTo(200f, 760f, 200f, 760f)
                        quadTo(200f, 760f, 200f, 760f)
                        lineTo(760f, 760f)
                        quadTo(760f, 760f, 760f, 760f)
                        quadTo(760f, 760f, 760f, 760f)
                        lineTo(760f, 400f)
                        lineTo(200f, 400f)
                        close()
                        moveTo(200f, 320f)
                        lineTo(200f, 200f)
                        quadTo(200f, 200f, 200f, 200f)
                        quadTo(200f, 200f, 200f, 200f)
                        lineTo(200f, 200f)
                        quadTo(200f, 200f, 200f, 200f)
                        quadTo(200f, 200f, 200f, 200f)
                        lineTo(200f, 320f)
                        close()
                    }
                }.build()

        return _TopPanelOpen!!
    }

@Suppress("ObjectPropertyName")
private var _TopPanelOpen: ImageVector? = null
