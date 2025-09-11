package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.IndeterminateQuestionBox: ImageVector
    get() {
        if (_IndeterminateQuestionBox != null) {
            return _IndeterminateQuestionBox!!
        }
        _IndeterminateQuestionBox = ImageVector.Builder(
            name = "IndeterminateQuestionBox",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(200f, 840f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(120f, 760f)
                verticalLineToRelative(-120f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(160f, 600f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(200f, 640f)
                verticalLineToRelative(120f)
                horizontalLineToRelative(120f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(360f, 800f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(320f, 840f)
                lineTo(200f, 840f)
                close()
                moveTo(760f, 840f)
                lineTo(640f, 840f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(600f, 800f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(640f, 760f)
                horizontalLineToRelative(120f)
                verticalLineToRelative(-120f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(800f, 600f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(840f, 640f)
                verticalLineToRelative(120f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(760f, 840f)
                close()
                moveTo(120f, 200f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(200f, 120f)
                horizontalLineToRelative(120f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(360f, 160f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(320f, 200f)
                lineTo(200f, 200f)
                verticalLineToRelative(120f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(160f, 360f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(120f, 320f)
                verticalLineToRelative(-120f)
                close()
                moveTo(840f, 200f)
                verticalLineToRelative(120f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(800f, 360f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(760f, 320f)
                verticalLineToRelative(-120f)
                lineTo(640f, 200f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(600f, 160f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(640f, 120f)
                horizontalLineToRelative(120f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(840f, 200f)
                close()
                moveTo(480f, 720f)
                quadToRelative(21f, 0f, 35.5f, -14.5f)
                reflectiveQuadTo(530f, 670f)
                quadToRelative(0f, -21f, -14.5f, -35.5f)
                reflectiveQuadTo(480f, 620f)
                quadToRelative(-21f, 0f, -35.5f, 14.5f)
                reflectiveQuadTo(430f, 670f)
                quadToRelative(0f, 21f, 14.5f, 35.5f)
                reflectiveQuadTo(480f, 720f)
                close()
                moveTo(480f, 308f)
                quadToRelative(26f, 0f, 45.5f, 16f)
                reflectiveQuadToRelative(19.5f, 41f)
                quadToRelative(0f, 23f, -14.5f, 41f)
                reflectiveQuadTo(499f, 439f)
                quadToRelative(-26f, 23f, -39.5f, 43.5f)
                reflectiveQuadTo(444f, 532f)
                quadToRelative(-1f, 14f, 10f, 24.5f)
                reflectiveQuadToRelative(26f, 10.5f)
                quadToRelative(14f, 0f, 25.5f, -10f)
                reflectiveQuadToRelative(13.5f, -25f)
                quadToRelative(2f, -17f, 12f, -30f)
                reflectiveQuadToRelative(29f, -32f)
                quadToRelative(35f, -35f, 46.5f, -56.5f)
                reflectiveQuadTo(618f, 362f)
                quadToRelative(0f, -54f, -39f, -88f)
                reflectiveQuadToRelative(-99f, -34f)
                quadToRelative(-41f, 0f, -73.5f, 18.5f)
                reflectiveQuadTo(357f, 311f)
                quadToRelative(-6f, 12f, -0.5f, 24.5f)
                reflectiveQuadTo(375f, 353f)
                quadToRelative(13f, 5f, 26.5f, 0f)
                reflectiveQuadToRelative(21.5f, -16f)
                quadToRelative(11f, -14f, 25.5f, -21.5f)
                reflectiveQuadTo(480f, 308f)
                close()
            }
        }.build()

        return _IndeterminateQuestionBox!!
    }

@Suppress("ObjectPropertyName")
private var _IndeterminateQuestionBox: ImageVector? = null
