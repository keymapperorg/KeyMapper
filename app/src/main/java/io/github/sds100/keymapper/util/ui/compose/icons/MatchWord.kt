package io.github.sds100.keymapper.util.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.MatchWord: ImageVector
    get() {
        if (_MatchWord != null) {
            return _MatchWord!!
        }
        _MatchWord = ImageVector.Builder(
            name = "MatchWord",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(40f, 761f)
                lineTo(40f, 561f)
                lineTo(120f, 561f)
                lineTo(120f, 681f)
                lineTo(840f, 681f)
                lineTo(840f, 561f)
                lineTo(920f, 561f)
                lineTo(920f, 761f)
                lineTo(40f, 761f)
                close()
                moveTo(382f, 600f)
                lineTo(382f, 566f)
                lineTo(379f, 566f)
                quadTo(366f, 586f, 344f, 597.5f)
                quadTo(322f, 609f, 294f, 609f)
                quadTo(245f, 609f, 217f, 583.5f)
                quadTo(189f, 558f, 189f, 514f)
                quadTo(189f, 472f, 221.5f, 445.5f)
                quadTo(254f, 419f, 305f, 419f)
                quadTo(328f, 419f, 347.5f, 422.5f)
                quadTo(367f, 426f, 381f, 434f)
                lineTo(381f, 420f)
                quadTo(381f, 393f, 362.5f, 377f)
                quadTo(344f, 361f, 312f, 361f)
                quadTo(291f, 361f, 272.5f, 370f)
                quadTo(254f, 379f, 241f, 396f)
                lineTo(198f, 364f)
                quadTo(217f, 337f, 246f, 323f)
                quadTo(275f, 309f, 313f, 309f)
                quadTo(375f, 309f, 408f, 338.5f)
                quadTo(441f, 368f, 441f, 424f)
                lineTo(441f, 600f)
                lineTo(382f, 600f)
                close()
                moveTo(316f, 466f)
                quadTo(284f, 466f, 267f, 478.5f)
                quadTo(250f, 491f, 250f, 514f)
                quadTo(250f, 534f, 265f, 546.5f)
                quadTo(280f, 559f, 304f, 559f)
                quadTo(336f, 559f, 358.5f, 536.5f)
                quadTo(381f, 514f, 381f, 482f)
                quadTo(367f, 474f, 349f, 470f)
                quadTo(331f, 466f, 316f, 466f)
                close()
                moveTo(501f, 600f)
                lineTo(501f, 199f)
                lineTo(563f, 199f)
                lineTo(563f, 312f)
                lineTo(560f, 352f)
                lineTo(563f, 352f)
                quadTo(566f, 347f, 587f, 326.5f)
                quadTo(608f, 306f, 653f, 306f)
                quadTo(717f, 306f, 754f, 352f)
                quadTo(791f, 398f, 791f, 458f)
                quadTo(791f, 518f, 754.5f, 563.5f)
                quadTo(718f, 609f, 653f, 609f)
                quadTo(612f, 609f, 590.5f, 591f)
                quadTo(569f, 573f, 563f, 563f)
                lineTo(560f, 563f)
                lineTo(560f, 600f)
                lineTo(501f, 600f)
                close()
                moveTo(644f, 362f)
                quadTo(604f, 362f, 582f, 391.5f)
                quadTo(560f, 421f, 560f, 457f)
                quadTo(560f, 494f, 582f, 523f)
                quadTo(604f, 552f, 644f, 552f)
                quadTo(684f, 552f, 706.5f, 523f)
                quadTo(729f, 494f, 729f, 457f)
                quadTo(729f, 420f, 706.5f, 391f)
                quadTo(684f, 362f, 644f, 362f)
                close()
            }
        }.build()

        return _MatchWord!!
    }

@Suppress("ObjectPropertyName")
private var _MatchWord: ImageVector? = null
