package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.InstantMix: ImageVector
    get() {
        if (_InstantMix != null) {
            return _InstantMix!!
        }
        _InstantMix =
            ImageVector
                .Builder(
                    name = "InstantMix",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 960f,
                    viewportHeight = 960f,
                ).apply {
                    path(fill = SolidColor(Color(0xFF000000))) {
                        moveTo(200f, 800f)
                        lineTo(200f, 520f)
                        lineTo(120f, 520f)
                        lineTo(120f, 440f)
                        lineTo(360f, 440f)
                        lineTo(360f, 520f)
                        lineTo(280f, 520f)
                        lineTo(280f, 800f)
                        lineTo(200f, 800f)
                        close()
                        moveTo(200f, 360f)
                        lineTo(200f, 160f)
                        lineTo(280f, 160f)
                        lineTo(280f, 360f)
                        lineTo(200f, 360f)
                        close()
                        moveTo(360f, 360f)
                        lineTo(360f, 280f)
                        lineTo(440f, 280f)
                        lineTo(440f, 160f)
                        lineTo(520f, 160f)
                        lineTo(520f, 280f)
                        lineTo(600f, 280f)
                        lineTo(600f, 360f)
                        lineTo(360f, 360f)
                        close()
                        moveTo(440f, 800f)
                        lineTo(440f, 440f)
                        lineTo(520f, 440f)
                        lineTo(520f, 800f)
                        lineTo(440f, 800f)
                        close()
                        moveTo(680f, 800f)
                        lineTo(680f, 680f)
                        lineTo(600f, 680f)
                        lineTo(600f, 600f)
                        lineTo(840f, 600f)
                        lineTo(840f, 680f)
                        lineTo(760f, 680f)
                        lineTo(760f, 800f)
                        lineTo(680f, 800f)
                        close()
                        moveTo(680f, 520f)
                        lineTo(680f, 160f)
                        lineTo(760f, 160f)
                        lineTo(760f, 520f)
                        lineTo(680f, 520f)
                        close()
                    }
                }.build()

        return _InstantMix!!
    }

@Suppress("ObjectPropertyName")
private var _InstantMix: ImageVector? = null
