package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.MobileSensorHi: ImageVector
    get() {
        if (_MobileSensorHi != null) {
            return _MobileSensorHi!!
        }
        _MobileSensorHi = ImageVector.Builder(
            name = "MobileSensorHi",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(320f, 840f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(240f, 760f)
                verticalLineToRelative(-560f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(320f, 120f)
                horizontalLineToRelative(320f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(720f, 200f)
                verticalLineToRelative(560f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(640f, 840f)
                lineTo(320f, 840f)
                close()
                moveTo(640f, 760f)
                verticalLineToRelative(-560f)
                lineTo(320f, 200f)
                verticalLineToRelative(560f)
                horizontalLineToRelative(320f)
                close()
                moveTo(508.5f, 308.5f)
                quadTo(520f, 297f, 520f, 280f)
                reflectiveQuadToRelative(-11.5f, -28.5f)
                quadTo(497f, 240f, 480f, 240f)
                reflectiveQuadToRelative(-28.5f, 11.5f)
                quadTo(440f, 263f, 440f, 280f)
                reflectiveQuadToRelative(11.5f, 28.5f)
                quadTo(463f, 320f, 480f, 320f)
                reflectiveQuadToRelative(28.5f, -11.5f)
                close()
                moveTo(0f, 680f)
                verticalLineToRelative(-280f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(280f)
                lineTo(0f, 680f)
                close()
                moveTo(120f, 560f)
                verticalLineToRelative(-280f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(280f)
                horizontalLineToRelative(-80f)
                close()
                moveTo(880f, 560f)
                verticalLineToRelative(-280f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(280f)
                horizontalLineToRelative(-80f)
                close()
                moveTo(760f, 680f)
                verticalLineToRelative(-280f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(280f)
                horizontalLineToRelative(-80f)
                close()
                moveTo(320f, 760f)
                verticalLineToRelative(-560f)
                verticalLineToRelative(560f)
                close()
            }
        }.build()

        return _MobileSensorHi!!
    }

@Suppress("ObjectPropertyName")
private var _MobileSensorHi: ImageVector? = null
