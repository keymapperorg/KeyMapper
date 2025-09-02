package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.Import: ImageVector
    get() {
        if (_Import != null) {
            return _Import!!
        }
        _Import = ImageVector.Builder(
            name = "Import",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveToRelative(240f, 920f)
                curveToRelative(-22f, 0f, -40.83f, -7.83f, -56.5f, -23.5f)
                curveTo(167.83f, 880.83f, 160f, 862f, 160f, 840f)
                verticalLineToRelative(-440f)
                curveToRelative(0f, -22f, 7.83f, -40.83f, 23.5f, -56.5f)
                curveToRelative(15.67f, -15.67f, 34.5f, -23.5f, 56.5f, -23.5f)
                horizontalLineToRelative(80f)
                curveToRelative(11.33f, 0f, 20.83f, 3.83f, 28.5f, 11.5f)
                curveToRelative(7.67f, 7.67f, 11.5f, 17.17f, 11.5f, 28.5f)
                curveToRelative(0f, 11.33f, -3.83f, 20.83f, -11.5f, 28.5f)
                curveToRelative(-7.67f, 7.67f, -17.17f, 11.5f, -28.5f, 11.5f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(440f)
                horizontalLineToRelative(480f)
                verticalLineToRelative(-440f)
                horizontalLineToRelative(-80f)
                curveToRelative(-11.33f, 0f, -20.83f, -3.83f, -28.5f, -11.5f)
                curveToRelative(-7.67f, -7.67f, -11.5f, -17.17f, -11.5f, -28.5f)
                curveToRelative(0f, -11.33f, 3.83f, -20.83f, 11.5f, -28.5f)
                curveToRelative(7.67f, -7.67f, 17.17f, -11.5f, 28.5f, -11.5f)
                horizontalLineToRelative(80f)
                curveToRelative(22f, 0f, 40.83f, 7.83f, 56.5f, 23.5f)
                curveToRelative(15.67f, 15.67f, 23.5f, 34.5f, 23.5f, 56.5f)
                verticalLineToRelative(440f)
                curveToRelative(0f, 22f, -7.83f, 40.83f, -23.5f, 56.5f)
                curveTo(760.83f, 912.17f, 742f, 920f, 720f, 920f)
                close()
                moveTo(440f, 503f)
                lineTo(404f, 467f)
                curveToRelative(-8f, -8f, -17.33f, -11.83f, -28f, -11.5f)
                curveToRelative(-10.67f, 0.33f, -20f, 4.5f, -28f, 12.5f)
                curveToRelative(-7.33f, 8f, -11.17f, 17.33f, -11.5f, 28f)
                curveToRelative(-0.33f, 10.67f, 3.5f, 20f, 11.5f, 28f)
                lineToRelative(104f, 104f)
                curveToRelative(8f, 8f, 17.33f, 12f, 28f, 12f)
                curveToRelative(10.67f, 0f, 20f, -4f, 28f, -12f)
                lineToRelative(104f, -104f)
                curveToRelative(7.33f, -7.33f, 11f, -16.5f, 11f, -27.5f)
                curveToRelative(0f, -11f, -3.67f, -20.5f, -11f, -28.5f)
                curveToRelative(-8f, -8f, -17.5f, -12f, -28.5f, -12f)
                curveToRelative(-11f, 0f, -20.5f, 4f, -28.5f, 12f)
                lineToRelative(-35f, 35f)
                verticalLineToRelative(-407f)
                curveToRelative(0f, -11.33f, -3.83f, -20.83f, -11.5f, -28.5f)
                curveToRelative(-7.67f, -7.67f, -17.17f, -11.5f, -28.5f, -11.5f)
                curveToRelative(-11.33f, 0f, -20.83f, 3.83f, -28.5f, 11.5f)
                curveToRelative(-7.67f, 7.67f, -11.5f, 17.17f, -11.5f, 28.5f)
                close()
            }
        }.build()

        return _Import!!
    }

@Suppress("ObjectPropertyName")
private var _Import: ImageVector? = null
