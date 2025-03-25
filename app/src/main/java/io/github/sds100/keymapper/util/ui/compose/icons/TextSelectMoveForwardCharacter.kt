package io.github.sds100.keymapper.util.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.TextSelectMoveForwardCharacter: ImageVector
    get() {
        if (_TextSelectMoveForwardCharacter != null) {
            return _TextSelectMoveForwardCharacter!!
        }
        _TextSelectMoveForwardCharacter = ImageVector.Builder(
            name = "TextSelectMoveForwardCharacter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(440f, 840f)
                lineTo(440f, 760f)
                lineTo(520f, 760f)
                lineTo(520f, 840f)
                lineTo(440f, 840f)
                close()
                moveTo(440f, 200f)
                lineTo(440f, 120f)
                lineTo(520f, 120f)
                lineTo(520f, 200f)
                lineTo(440f, 200f)
                close()
                moveTo(600f, 840f)
                lineTo(600f, 760f)
                lineTo(680f, 760f)
                lineTo(680f, 840f)
                lineTo(600f, 840f)
                close()
                moveTo(600f, 200f)
                lineTo(600f, 120f)
                lineTo(680f, 120f)
                lineTo(680f, 200f)
                lineTo(600f, 200f)
                close()
                moveTo(760f, 840f)
                lineTo(760f, 760f)
                lineTo(840f, 760f)
                lineTo(840f, 840f)
                lineTo(760f, 840f)
                close()
                moveTo(760f, 200f)
                lineTo(760f, 120f)
                lineTo(840f, 120f)
                lineTo(840f, 200f)
                lineTo(760f, 200f)
                close()
                moveTo(120f, 840f)
                lineTo(120f, 760f)
                lineTo(200f, 760f)
                lineTo(200f, 200f)
                lineTo(120f, 200f)
                lineTo(120f, 120f)
                lineTo(360f, 120f)
                lineTo(360f, 200f)
                lineTo(280f, 200f)
                lineTo(280f, 760f)
                lineTo(360f, 760f)
                lineTo(360f, 840f)
                lineTo(120f, 840f)
                close()
                moveTo(680f, 640f)
                lineTo(624f, 584f)
                lineTo(687f, 520f)
                lineTo(400f, 520f)
                lineTo(400f, 440f)
                lineTo(687f, 440f)
                lineTo(624f, 376f)
                lineTo(680f, 320f)
                lineTo(840f, 480f)
                lineTo(680f, 640f)
                close()
            }
        }.build()

        return _TextSelectMoveForwardCharacter!!
    }

@Suppress("ObjectPropertyName")
private var _TextSelectMoveForwardCharacter: ImageVector? = null
