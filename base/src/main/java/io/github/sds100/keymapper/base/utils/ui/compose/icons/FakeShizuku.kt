package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.FakeShizuku: ImageVector
    get() {
        if (_FakeShizuku != null) {
            return _FakeShizuku!!
        }
        _FakeShizuku =
            ImageVector
                .Builder(
                    name = "FakeShizuku",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                ).apply {
                    path(fill = SolidColor(Color(0xFF4053B9))) {
                        moveTo(12f, 24f)
                        curveTo(18.627f, 24f, 24f, 18.627f, 24f, 12f)
                        curveTo(24f, 5.373f, 18.627f, 0f, 12f, 0f)
                        curveTo(5.373f, 0f, 0f, 5.373f, 0f, 12f)
                        curveTo(0f, 18.627f, 5.373f, 24f, 12f, 24f)
                        close()
                    }
                    path(fill = SolidColor(Color(0xFF717DC0))) {
                        moveTo(15.384f, 17.771f)
                        lineTo(8.74f, 17.752f)
                        lineTo(5.434f, 11.989f)
                        lineTo(8.772f, 6.244f)
                        lineTo(15.416f, 6.263f)
                        lineTo(18.722f, 12.026f)
                        lineTo(15.384f, 17.771f)
                        close()
                    }
                }.build()

        return _FakeShizuku!!
    }

@Suppress("ObjectPropertyName")
private var _FakeShizuku: ImageVector? = null
