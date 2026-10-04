package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.KeyboardPreviousLanguage: ImageVector
    get() {
        if (_KeyboardPreviousLanguage != null) {
            return _KeyboardPreviousLanguage!!
        }
        _KeyboardPreviousLanguage = ImageVector.Builder(
            name = "KeyboardPreviousLanguage",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(160f, 880f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(80f, 800f)
                verticalLineToRelative(-400f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(160f, 320f)
                horizontalLineToRelative(640f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(880f, 400f)
                verticalLineToRelative(400f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(800f, 880f)
                lineTo(160f, 880f)
                close()
                moveTo(160f, 800f)
                horizontalLineToRelative(640f)
                verticalLineToRelative(-400f)
                lineTo(160f, 400f)
                verticalLineToRelative(400f)
                close()
                moveTo(320f, 760f)
                horizontalLineToRelative(320f)
                verticalLineToRelative(-80f)
                lineTo(320f, 680f)
                verticalLineToRelative(80f)
                close()
                moveTo(200f, 640f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(80f)
                close()
                moveTo(320f, 640f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(80f)
                close()
                moveTo(440f, 640f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(80f)
                close()
                moveTo(560f, 640f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(80f)
                close()
                moveTo(680f, 640f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(80f)
                close()
                moveTo(200f, 520f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(80f)
                close()
                moveTo(320f, 520f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(80f)
                close()
                moveTo(440f, 520f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(80f)
                close()
                moveTo(560f, 520f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(80f)
                close()
                moveTo(680f, 520f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(80f)
                close()
                moveTo(160f, 800f)
                verticalLineToRelative(-400f)
                verticalLineToRelative(400f)
                close()
                moveTo(240f, 240f)
                verticalLineToRelative(-200f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(61f)
                quadToRelative(32f, -29f, 73f, -45f)
                reflectiveQuadToRelative(87f, -16f)
                quadToRelative(88f, 0f, 155f, 56.5f)
                reflectiveQuadTo(716f, 240f)
                horizontalLineToRelative(-82f)
                quadToRelative(-14f, -53f, -56.5f, -86.5f)
                reflectiveQuadTo(480f, 120f)
                quadToRelative(-30f, 0f, -57f, 10.5f)
                reflectiveQuadTo(375f, 160f)
                horizontalLineToRelative(65f)
                verticalLineToRelative(80f)
                lineTo(240f, 240f)
                close()
            }
        }.build()

        return _KeyboardPreviousLanguage!!
    }

@Suppress("ObjectPropertyName")
private var _KeyboardPreviousLanguage: ImageVector? = null
