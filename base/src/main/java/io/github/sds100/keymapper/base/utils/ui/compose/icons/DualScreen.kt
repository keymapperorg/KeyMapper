package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.DualScreen: ImageVector
    get() {
        if (_DualScreen != null) {
            return _DualScreen!!
        }
        _DualScreen = ImageVector.Builder(
            name = "DualScreen",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveToRelative(240f, 704f)
                lineToRelative(240f, 96f)
                verticalLineToRelative(-544f)
                lineToRelative(-240f, -96f)
                verticalLineToRelative(544f)
                close()
                moveTo(210f, 778f)
                quadToRelative(-23f, -9f, -36.5f, -29f)
                reflectiveQuadTo(160f, 704f)
                verticalLineToRelative(-544f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(240f, 80f)
                lineToRelative(268f, 101f)
                quadToRelative(23f, 9f, 37.5f, 29.5f)
                reflectiveQuadTo(560f, 256f)
                verticalLineToRelative(544f)
                quadToRelative(0f, 43f, -35f, 66.5f)
                reflectiveQuadTo(450f, 874f)
                lineToRelative(-240f, -96f)
                close()
                moveTo(480f, 760f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(240f)
                verticalLineToRelative(-520f)
                lineTo(240f, 160f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(480f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(800f, 160f)
                verticalLineToRelative(520f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(720f, 760f)
                lineTo(480f, 760f)
                close()
                moveTo(240f, 704f)
                verticalLineToRelative(-544f)
                verticalLineToRelative(544f)
                close()
            }
        }.build()

        return _DualScreen!!
    }

@Suppress("ObjectPropertyName")
private var _DualScreen: ImageVector? = null
