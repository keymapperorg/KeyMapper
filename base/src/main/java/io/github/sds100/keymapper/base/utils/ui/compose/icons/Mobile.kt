package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.Mobile: ImageVector
    get() {
        if (_Mobile != null) {
            return _Mobile!!
        }
        _Mobile = ImageVector.Builder(
            name = "Mobile",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(280f, 920f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(200f, 840f)
                verticalLineToRelative(-720f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(280f, 40f)
                horizontalLineToRelative(400f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(760f, 120f)
                verticalLineToRelative(124f)
                quadToRelative(18f, 7f, 29f, 22f)
                reflectiveQuadToRelative(11f, 34f)
                verticalLineToRelative(80f)
                quadToRelative(0f, 19f, -11f, 34f)
                reflectiveQuadToRelative(-29f, 22f)
                verticalLineToRelative(404f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(680f, 920f)
                lineTo(280f, 920f)
                close()
                moveTo(280f, 840f)
                horizontalLineToRelative(400f)
                verticalLineToRelative(-720f)
                lineTo(280f, 120f)
                verticalLineToRelative(720f)
                close()
                moveTo(280f, 840f)
                verticalLineToRelative(-720f)
                verticalLineToRelative(720f)
                close()
                moveTo(508.5f, 228.5f)
                quadTo(520f, 217f, 520f, 200f)
                reflectiveQuadToRelative(-11.5f, -28.5f)
                quadTo(497f, 160f, 480f, 160f)
                reflectiveQuadToRelative(-28.5f, 11.5f)
                quadTo(440f, 183f, 440f, 200f)
                reflectiveQuadToRelative(11.5f, 28.5f)
                quadTo(463f, 240f, 480f, 240f)
                reflectiveQuadToRelative(28.5f, -11.5f)
                close()
            }
        }.build()

        return _Mobile!!
    }

@Suppress("ObjectPropertyName")
private var _Mobile: ImageVector? = null
