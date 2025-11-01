package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.VoiceSelection: ImageVector
    get() {
        if (_VoiceSelection != null) {
            return _VoiceSelection!!
        }
        _VoiceSelection = ImageVector.Builder(
            name = "VoiceSelection",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(737f, 892f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(697f, 852f)
                quadToRelative(0f, -8f, 3f, -16f)
                reflectiveQuadToRelative(8f, -13f)
                quadToRelative(43f, -43f, 67.5f, -99f)
                reflectiveQuadTo(800f, 602f)
                quadToRelative(0f, -65f, -24.5f, -121.5f)
                reflectiveQuadTo(708f, 381f)
                quadToRelative(-5f, -5f, -8f, -13f)
                reflectiveQuadToRelative(-3f, -16f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(737f, 312f)
                quadToRelative(8f, 0f, 15.5f, 3.5f)
                reflectiveQuadTo(765f, 324f)
                quadToRelative(54f, 54f, 84.5f, 125.5f)
                reflectiveQuadTo(880f, 602f)
                quadToRelative(0f, 82f, -30.5f, 153f)
                reflectiveQuadTo(765f, 880f)
                quadToRelative(-5f, 5f, -12.5f, 8.5f)
                reflectiveQuadTo(737f, 892f)
                close()
                moveTo(623f, 778f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(583f, 738f)
                quadToRelative(0f, -8f, 3.5f, -15.5f)
                reflectiveQuadTo(595f, 710f)
                quadToRelative(21f, -21f, 33f, -48.5f)
                reflectiveQuadToRelative(12f, -59.5f)
                quadToRelative(0f, -32f, -12.5f, -59.5f)
                reflectiveQuadTo(595f, 494f)
                quadToRelative(-6f, -5f, -9f, -12.5f)
                reflectiveQuadToRelative(-3f, -15.5f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(623f, 426f)
                quadToRelative(8f, 0f, 16f, 3f)
                reflectiveQuadToRelative(13f, 9f)
                quadToRelative(32f, 32f, 50f, 74f)
                reflectiveQuadToRelative(18f, 90f)
                quadToRelative(0f, 48f, -18f, 90f)
                reflectiveQuadToRelative(-50f, 74f)
                quadToRelative(-5f, 6f, -13f, 9f)
                reflectiveQuadToRelative(-16f, 3f)
                close()
                moveTo(320f, 600f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(11f)
                quadToRelative(0f, 35f, 21.5f, 61.5f)
                reflectiveQuadTo(316f, 708f)
                lineToRelative(12f, 3f)
                quadToRelative(40f, 10f, 45f, 50f)
                reflectiveQuadToRelative(-31f, 60f)
                quadToRelative(-51f, 29f, -107f, 42f)
                reflectiveQuadTo(120f, 879f)
                quadToRelative(-17f, 1f, -28.5f, -10.5f)
                reflectiveQuadTo(80f, 840f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(120f, 799f)
                quadToRelative(36f, -2f, 70.5f, -8.5f)
                reflectiveQuadTo(259f, 772f)
                quadToRelative(-46f, -23f, -72.5f, -66.5f)
                reflectiveQuadTo(160f, 611f)
                verticalLineToRelative(-51f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(200f, 520f)
                horizontalLineToRelative(120f)
                verticalLineToRelative(-80f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(360f, 400f)
                horizontalLineToRelative(95f)
                lineTo(342f, 174f)
                quadToRelative(-8f, -15f, -2.5f, -30.5f)
                reflectiveQuadTo(360f, 120f)
                quadToRelative(15f, -8f, 30.5f, -2.5f)
                reflectiveQuadTo(414f, 138f)
                lineToRelative(113f, 226f)
                quadToRelative(20f, 40f, -3f, 78f)
                reflectiveQuadToRelative(-68f, 38f)
                horizontalLineToRelative(-56f)
                verticalLineToRelative(40f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(320f, 600f)
                close()
            }
        }.build()

        return _VoiceSelection!!
    }

@Suppress("ObjectPropertyName")
private var _VoiceSelection: ImageVector? = null
