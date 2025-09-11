package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.ModeOffOn: ImageVector
    get() {
        if (_ModeOffOn != null) {
            return _ModeOffOn!!
        }
        _ModeOffOn = ImageVector.Builder(
            name = "ModeOffOn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(480f, 480f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(440f, 440f)
                verticalLineToRelative(-320f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(480f, 80f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(520f, 120f)
                verticalLineToRelative(320f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(480f, 480f)
                close()
                moveTo(480f, 840f)
                quadToRelative(-75f, 0f, -140.5f, -28.5f)
                reflectiveQuadToRelative(-114f, -77f)
                quadToRelative(-48.5f, -48.5f, -77f, -114f)
                reflectiveQuadTo(120f, 480f)
                quadToRelative(0f, -61f, 20f, -118.5f)
                reflectiveQuadTo(198f, 256f)
                quadToRelative(11f, -14f, 28f, -13.5f)
                reflectiveQuadToRelative(30f, 13.5f)
                quadToRelative(11f, 11f, 10f, 27f)
                reflectiveQuadToRelative(-11f, 30f)
                quadToRelative(-27f, 36f, -41f, 79f)
                reflectiveQuadToRelative(-14f, 88f)
                quadToRelative(0f, 117f, 81.5f, 198.5f)
                reflectiveQuadTo(480f, 760f)
                quadToRelative(117f, 0f, 198.5f, -81.5f)
                reflectiveQuadTo(760f, 480f)
                quadToRelative(0f, -46f, -13.5f, -89.5f)
                reflectiveQuadTo(704f, 311f)
                quadToRelative(-10f, -13f, -11f, -28.5f)
                reflectiveQuadToRelative(10f, -26.5f)
                quadToRelative(12f, -12f, 29f, -12.5f)
                reflectiveQuadToRelative(28f, 12.5f)
                quadToRelative(39f, 48f, 59.5f, 105f)
                reflectiveQuadTo(840f, 480f)
                quadToRelative(0f, 75f, -28.5f, 140.5f)
                reflectiveQuadToRelative(-77f, 114f)
                quadToRelative(-48.5f, 48.5f, -114f, 77f)
                reflectiveQuadTo(480f, 840f)
                close()
            }
        }.build()

        return _ModeOffOn!!
    }

@Suppress("ObjectPropertyName")
private var _ModeOffOn: ImageVector? = null
