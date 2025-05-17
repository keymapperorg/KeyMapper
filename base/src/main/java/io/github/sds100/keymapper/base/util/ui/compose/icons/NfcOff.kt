package io.github.sds100.keymapper.base.util.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.NfcOff: ImageVector
    get() {
        if (_NfcOff != null) {
            return _NfcOff!!
        }
        _NfcOff = ImageVector.Builder(
            name = "NfcOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(1.25f, 2.05f)
                lineTo(21.95f, 22.75f)
                lineTo(20.7f, 24f)
                lineTo(18.7f, 22f)
                horizontalLineTo(4f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, 20f)
                verticalLineTo(5.3f)
                lineTo(0f, 3.3f)
                lineTo(1.25f, 2.05f)
                moveTo(3.81f, 2f)
                curveTo(3.87f, 2f, 3.94f, 2f, 4f, 2f)
                horizontalLineTo(20f)
                curveTo(21.11f, 2f, 22f, 2.89f, 22f, 4f)
                verticalLineTo(20f)
                curveTo(22f, 20.06f, 22f, 20.13f, 22f, 20.19f)
                lineTo(20f, 18.2f)
                verticalLineTo(4f)
                horizontalLineTo(5.8f)
                lineTo(3.81f, 2f)
                moveTo(6f, 9.3f)
                lineTo(4f, 7.3f)
                verticalLineTo(20f)
                horizontalLineTo(16.7f)
                lineTo(14.7f, 18f)
                horizontalLineTo(6f)
                verticalLineTo(9.3f)
                moveTo(18f, 16.2f)
                lineTo(16f, 14.2f)
                verticalLineTo(8f)
                horizontalLineTo(13f)
                verticalLineTo(10.28f)
                curveTo(13.6f, 10.62f, 14f, 11.26f, 14f, 12f)
                curveTo(14f, 12.06f, 14f, 12.13f, 14f, 12.19f)
                lineTo(11f, 9.2f)
                verticalLineTo(8f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 13f, 6f)
                horizontalLineTo(18f)
                verticalLineTo(16.2f)
                moveTo(8f, 16f)
                horizontalLineTo(12.7f)
                lineTo(8f, 11.3f)
                verticalLineTo(16f)
                moveTo(10f, 8f)
                horizontalLineTo(9.8f)
                lineTo(7.8f, 6f)
                horizontalLineTo(10f)
                verticalLineTo(8f)
                close()
            }
        }.build()

        return _NfcOff!!
    }

@Suppress("ObjectPropertyName")
private var _NfcOff: ImageVector? = null
