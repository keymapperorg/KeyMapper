package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.ActionKey: ImageVector
    get() {
        if (_ActionKey != null) {
            return _ActionKey!!
        }
        _ActionKey = ImageVector.Builder(
            name = "ActionKey",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(864f, 920f)
                lineTo(741f, 798f)
                quadToRelative(-18f, 11f, -38.5f, 16.5f)
                reflectiveQuadTo(660f, 820f)
                quadToRelative(-66f, 0f, -113f, -47f)
                reflectiveQuadToRelative(-47f, -113f)
                quadToRelative(0f, -66f, 47f, -113f)
                reflectiveQuadToRelative(113f, -47f)
                quadToRelative(66f, 0f, 113f, 47f)
                reflectiveQuadToRelative(47f, 113f)
                quadToRelative(0f, 23f, -6f, 43.5f)
                reflectiveQuadTo(797f, 742f)
                lineTo(920f, 864f)
                lineToRelative(-56f, 56f)
                close()
                moveTo(220f, 820f)
                quadToRelative(-66f, 0f, -113f, -47f)
                reflectiveQuadTo(60f, 660f)
                quadToRelative(0f, -66f, 47f, -113f)
                reflectiveQuadToRelative(113f, -47f)
                quadToRelative(66f, 0f, 113f, 47f)
                reflectiveQuadToRelative(47f, 113f)
                quadToRelative(0f, 66f, -47f, 113f)
                reflectiveQuadToRelative(-113f, 47f)
                close()
                moveTo(220f, 740f)
                quadToRelative(33f, 0f, 56.5f, -23.5f)
                reflectiveQuadTo(300f, 660f)
                quadToRelative(0f, -33f, -23.5f, -56.5f)
                reflectiveQuadTo(220f, 580f)
                quadToRelative(-33f, 0f, -56.5f, 23.5f)
                reflectiveQuadTo(140f, 660f)
                quadToRelative(0f, 33f, 23.5f, 56.5f)
                reflectiveQuadTo(220f, 740f)
                close()
                moveTo(660f, 740f)
                quadToRelative(33f, 0f, 56.5f, -23.5f)
                reflectiveQuadTo(740f, 660f)
                quadToRelative(0f, -33f, -23.5f, -56.5f)
                reflectiveQuadTo(660f, 580f)
                quadToRelative(-33f, 0f, -56.5f, 23.5f)
                reflectiveQuadTo(580f, 660f)
                quadToRelative(0f, 33f, 23.5f, 56.5f)
                reflectiveQuadTo(660f, 740f)
                close()
                moveTo(220f, 380f)
                quadToRelative(-66f, 0f, -113f, -47f)
                reflectiveQuadTo(60f, 220f)
                quadToRelative(0f, -66f, 47f, -113f)
                reflectiveQuadToRelative(113f, -47f)
                quadToRelative(66f, 0f, 113f, 47f)
                reflectiveQuadToRelative(47f, 113f)
                quadToRelative(0f, 66f, -47f, 113f)
                reflectiveQuadToRelative(-113f, 47f)
                close()
                moveTo(660f, 380f)
                quadToRelative(-66f, 0f, -113f, -47f)
                reflectiveQuadToRelative(-47f, -113f)
                quadToRelative(0f, -66f, 47f, -113f)
                reflectiveQuadToRelative(113f, -47f)
                quadToRelative(66f, 0f, 113f, 47f)
                reflectiveQuadToRelative(47f, 113f)
                quadToRelative(0f, 66f, -47f, 113f)
                reflectiveQuadToRelative(-113f, 47f)
                close()
                moveTo(220f, 300f)
                quadToRelative(33f, 0f, 56.5f, -23.5f)
                reflectiveQuadTo(300f, 220f)
                quadToRelative(0f, -33f, -23.5f, -56.5f)
                reflectiveQuadTo(220f, 140f)
                quadToRelative(-33f, 0f, -56.5f, 23.5f)
                reflectiveQuadTo(140f, 220f)
                quadToRelative(0f, 33f, 23.5f, 56.5f)
                reflectiveQuadTo(220f, 300f)
                close()
                moveTo(660f, 300f)
                quadToRelative(33f, 0f, 56.5f, -23.5f)
                reflectiveQuadTo(740f, 220f)
                quadToRelative(0f, -33f, -23.5f, -56.5f)
                reflectiveQuadTo(660f, 140f)
                quadToRelative(-33f, 0f, -56.5f, 23.5f)
                reflectiveQuadTo(580f, 220f)
                quadToRelative(0f, 33f, 23.5f, 56.5f)
                reflectiveQuadTo(660f, 300f)
                close()
                moveTo(220f, 660f)
                close()
                moveTo(220f, 220f)
                close()
                moveTo(660f, 220f)
                close()
            }
        }.build()

        return _ActionKey!!
    }

@Suppress("ObjectPropertyName")
private var _ActionKey: ImageVector? = null
