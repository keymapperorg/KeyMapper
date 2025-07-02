package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.AdGroup: ImageVector
    get() {
        if (_AdGroup != null) {
            return _AdGroup!!
        }
        _AdGroup = ImageVector.Builder(
            name = "AdGroup",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(320f, 640f)
                lineTo(800f, 640f)
                quadTo(800f, 640f, 800f, 640f)
                quadTo(800f, 640f, 800f, 640f)
                lineTo(800f, 240f)
                lineTo(320f, 240f)
                lineTo(320f, 640f)
                quadTo(320f, 640f, 320f, 640f)
                quadTo(320f, 640f, 320f, 640f)
                close()
                moveTo(320f, 720f)
                quadTo(287f, 720f, 263.5f, 696.5f)
                quadTo(240f, 673f, 240f, 640f)
                lineTo(240f, 160f)
                quadTo(240f, 127f, 263.5f, 103.5f)
                quadTo(287f, 80f, 320f, 80f)
                lineTo(800f, 80f)
                quadTo(833f, 80f, 856.5f, 103.5f)
                quadTo(880f, 127f, 880f, 160f)
                lineTo(880f, 640f)
                quadTo(880f, 673f, 856.5f, 696.5f)
                quadTo(833f, 720f, 800f, 720f)
                lineTo(320f, 720f)
                close()
                moveTo(160f, 880f)
                quadTo(127f, 880f, 103.5f, 856.5f)
                quadTo(80f, 833f, 80f, 800f)
                lineTo(80f, 240f)
                lineTo(160f, 240f)
                lineTo(160f, 800f)
                quadTo(160f, 800f, 160f, 800f)
                quadTo(160f, 800f, 160f, 800f)
                lineTo(720f, 800f)
                lineTo(720f, 880f)
                lineTo(160f, 880f)
                close()
                moveTo(320f, 160f)
                quadTo(320f, 160f, 320f, 160f)
                quadTo(320f, 160f, 320f, 160f)
                lineTo(320f, 640f)
                quadTo(320f, 640f, 320f, 640f)
                quadTo(320f, 640f, 320f, 640f)
                lineTo(320f, 640f)
                quadTo(320f, 640f, 320f, 640f)
                quadTo(320f, 640f, 320f, 640f)
                lineTo(320f, 160f)
                quadTo(320f, 160f, 320f, 160f)
                quadTo(320f, 160f, 320f, 160f)
                close()
            }
        }.build()

        return _AdGroup!!
    }

@Suppress("ObjectPropertyName")
private var _AdGroup: ImageVector? = null
