package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.FolderManaged: ImageVector
    get() {
        if (_FolderManaged != null) {
            return _FolderManaged!!
        }
        _FolderManaged = ImageVector.Builder(
            name = "FolderManaged",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(720f, 760f)
                quadToRelative(33f, 0f, 56.5f, -23.5f)
                reflectiveQuadTo(800f, 680f)
                quadToRelative(0f, -33f, -23.5f, -56.5f)
                reflectiveQuadTo(720f, 600f)
                quadToRelative(-33f, 0f, -56.5f, 23.5f)
                reflectiveQuadTo(640f, 680f)
                quadToRelative(0f, 33f, 23.5f, 56.5f)
                reflectiveQuadTo(720f, 760f)
                close()
                moveTo(712f, 880f)
                quadToRelative(-14f, 0f, -24.5f, -9f)
                reflectiveQuadTo(674f, 848f)
                lineToRelative(-6f, -28f)
                quadToRelative(-12f, -5f, -22.5f, -10.5f)
                reflectiveQuadTo(624f, 796f)
                lineToRelative(-29f, 9f)
                quadToRelative(-13f, 4f, -25.5f, -1f)
                reflectiveQuadTo(550f, 788f)
                lineToRelative(-8f, -14f)
                quadToRelative(-7f, -12f, -5f, -26f)
                reflectiveQuadToRelative(13f, -23f)
                lineToRelative(22f, -19f)
                quadToRelative(-2f, -12f, -2f, -26f)
                reflectiveQuadToRelative(2f, -26f)
                lineToRelative(-22f, -19f)
                quadToRelative(-11f, -9f, -13f, -22.5f)
                reflectiveQuadToRelative(5f, -25.5f)
                lineToRelative(9f, -15f)
                quadToRelative(7f, -11f, 19f, -16f)
                reflectiveQuadToRelative(25f, -1f)
                lineToRelative(29f, 9f)
                quadToRelative(11f, -8f, 21.5f, -13.5f)
                reflectiveQuadTo(668f, 540f)
                lineToRelative(6f, -29f)
                quadToRelative(3f, -14f, 13.5f, -22.5f)
                reflectiveQuadTo(712f, 480f)
                horizontalLineToRelative(16f)
                quadToRelative(14f, 0f, 24.5f, 9f)
                reflectiveQuadToRelative(13.5f, 23f)
                lineToRelative(6f, 28f)
                quadToRelative(12f, 5f, 22.5f, 10.5f)
                reflectiveQuadTo(816f, 564f)
                lineToRelative(29f, -9f)
                quadToRelative(13f, -4f, 25.5f, 1f)
                reflectiveQuadToRelative(19.5f, 16f)
                lineToRelative(8f, 14f)
                quadToRelative(7f, 12f, 5f, 26f)
                reflectiveQuadToRelative(-13f, 23f)
                lineToRelative(-22f, 19f)
                quadToRelative(2f, 12f, 2f, 26f)
                reflectiveQuadToRelative(-2f, 26f)
                lineToRelative(22f, 19f)
                quadToRelative(11f, 9f, 13f, 22.5f)
                reflectiveQuadToRelative(-5f, 25.5f)
                lineToRelative(-9f, 15f)
                quadToRelative(-7f, 11f, -19f, 16f)
                reflectiveQuadToRelative(-25f, 1f)
                lineToRelative(-29f, -9f)
                quadToRelative(-11f, 8f, -21.5f, 13.5f)
                reflectiveQuadTo(772f, 820f)
                lineToRelative(-6f, 29f)
                quadToRelative(-3f, 14f, -13.5f, 22.5f)
                reflectiveQuadTo(728f, 880f)
                horizontalLineToRelative(-16f)
                close()
                moveTo(160f, 720f)
                verticalLineToRelative(-480f)
                verticalLineToRelative(172f)
                verticalLineToRelative(-12f)
                verticalLineToRelative(320f)
                close()
                moveTo(160f, 800f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(80f, 720f)
                verticalLineToRelative(-480f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(160f, 160f)
                horizontalLineToRelative(207f)
                quadToRelative(16f, 0f, 30.5f, 6f)
                reflectiveQuadToRelative(25.5f, 17f)
                lineToRelative(57f, 57f)
                horizontalLineToRelative(320f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(880f, 320f)
                verticalLineToRelative(80f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(840f, 440f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(800f, 400f)
                verticalLineToRelative(-80f)
                lineTo(447f, 320f)
                lineToRelative(-80f, -80f)
                lineTo(160f, 240f)
                verticalLineToRelative(480f)
                horizontalLineToRelative(280f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(480f, 760f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(440f, 800f)
                lineTo(160f, 800f)
                close()
            }
        }.build()

        return _FolderManaged!!
    }

@Suppress("ObjectPropertyName")
private var _FolderManaged: ImageVector? = null
