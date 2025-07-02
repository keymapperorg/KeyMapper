package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.JumpToElement: ImageVector
    get() {
        if (_JumpToElement != null) {
            return _JumpToElement!!
        }
        _JumpToElement = ImageVector.Builder(
            name = "JumpToElement",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(520f, 440f)
                lineTo(560f, 440f)
                quadTo(577f, 440f, 588.5f, 451.5f)
                quadTo(600f, 463f, 600f, 480f)
                quadTo(600f, 497f, 588.5f, 508.5f)
                quadTo(577f, 520f, 560f, 520f)
                lineTo(480f, 520f)
                quadTo(463f, 520f, 451.5f, 508.5f)
                quadTo(440f, 497f, 440f, 480f)
                lineTo(440f, 400f)
                quadTo(440f, 383f, 451.5f, 371.5f)
                quadTo(463f, 360f, 480f, 360f)
                quadTo(497f, 360f, 508.5f, 371.5f)
                quadTo(520f, 383f, 520f, 400f)
                lineTo(520f, 440f)
                close()
                moveTo(800f, 440f)
                lineTo(800f, 400f)
                quadTo(800f, 383f, 811.5f, 371.5f)
                quadTo(823f, 360f, 840f, 360f)
                quadTo(857f, 360f, 868.5f, 371.5f)
                quadTo(880f, 383f, 880f, 400f)
                lineTo(880f, 480f)
                quadTo(880f, 497f, 868.5f, 508.5f)
                quadTo(857f, 520f, 840f, 520f)
                lineTo(760f, 520f)
                quadTo(743f, 520f, 731.5f, 508.5f)
                quadTo(720f, 497f, 720f, 480f)
                quadTo(720f, 463f, 731.5f, 451.5f)
                quadTo(743f, 440f, 760f, 440f)
                lineTo(800f, 440f)
                close()
                moveTo(520f, 160f)
                lineTo(520f, 200f)
                quadTo(520f, 217f, 508.5f, 228.5f)
                quadTo(497f, 240f, 480f, 240f)
                quadTo(463f, 240f, 451.5f, 228.5f)
                quadTo(440f, 217f, 440f, 200f)
                lineTo(440f, 120f)
                quadTo(440f, 103f, 451.5f, 91.5f)
                quadTo(463f, 80f, 480f, 80f)
                lineTo(560f, 80f)
                quadTo(577f, 80f, 588.5f, 91.5f)
                quadTo(600f, 103f, 600f, 120f)
                quadTo(600f, 137f, 588.5f, 148.5f)
                quadTo(577f, 160f, 560f, 160f)
                lineTo(520f, 160f)
                close()
                moveTo(800f, 160f)
                lineTo(760f, 160f)
                quadTo(743f, 160f, 731.5f, 148.5f)
                quadTo(720f, 137f, 720f, 120f)
                quadTo(720f, 103f, 731.5f, 91.5f)
                quadTo(743f, 80f, 760f, 80f)
                lineTo(840f, 80f)
                quadTo(857f, 80f, 868.5f, 91.5f)
                quadTo(880f, 103f, 880f, 120f)
                lineTo(880f, 200f)
                quadTo(880f, 217f, 868.5f, 228.5f)
                quadTo(857f, 240f, 840f, 240f)
                quadTo(823f, 240f, 811.5f, 228.5f)
                quadTo(800f, 217f, 800f, 200f)
                lineTo(800f, 160f)
                close()
                moveTo(360f, 656f)
                lineTo(164f, 852f)
                quadTo(153f, 863f, 136f, 863f)
                quadTo(119f, 863f, 108f, 852f)
                quadTo(97f, 841f, 97f, 824f)
                quadTo(97f, 807f, 108f, 796f)
                lineTo(304f, 600f)
                lineTo(160f, 600f)
                quadTo(143f, 600f, 131.5f, 588.5f)
                quadTo(120f, 577f, 120f, 560f)
                quadTo(120f, 543f, 131.5f, 531.5f)
                quadTo(143f, 520f, 160f, 520f)
                lineTo(400f, 520f)
                quadTo(417f, 520f, 428.5f, 531.5f)
                quadTo(440f, 543f, 440f, 560f)
                lineTo(440f, 800f)
                quadTo(440f, 817f, 428.5f, 828.5f)
                quadTo(417f, 840f, 400f, 840f)
                quadTo(383f, 840f, 371.5f, 828.5f)
                quadTo(360f, 817f, 360f, 800f)
                lineTo(360f, 656f)
                close()
            }
        }.build()

        return _JumpToElement!!
    }

@Suppress("ObjectPropertyName")
private var _JumpToElement: ImageVector? = null
