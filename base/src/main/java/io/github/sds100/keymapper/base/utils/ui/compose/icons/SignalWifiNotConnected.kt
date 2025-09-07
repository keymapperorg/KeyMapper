package io.github.sds100.keymapper.base.utils.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeyMapperIcons.SignalWifiNotConnected: ImageVector
    get() {
        if (_SignalWifiNotConnected != null) {
            return _SignalWifiNotConnected!!
        }
        _SignalWifiNotConnected = ImageVector.Builder(
            name = "SignalWifiNotConnected",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(423f, 783f)
                lineTo(61f, 421f)
                quadToRelative(-13f, -13f, -18.5f, -28f)
                reflectiveQuadTo(38f, 361f)
                quadToRelative(1f, -17f, 7f, -32f)
                reflectiveQuadToRelative(20f, -26f)
                quadToRelative(81f, -71f, 194.5f, -107f)
                reflectiveQuadTo(480f, 160f)
                quadToRelative(125f, 0f, 234f, 41f)
                reflectiveQuadToRelative(203f, 122f)
                quadToRelative(9f, 8f, 13.5f, 17.5f)
                reflectiveQuadTo(935f, 361f)
                quadToRelative(0f, 11f, -3.5f, 21f)
                reflectiveQuadTo(920f, 400f)
                quadToRelative(-28f, -36f, -69.5f, -58f)
                reflectiveQuadTo(760f, 320f)
                quadToRelative(-83f, 0f, -141.5f, 58.5f)
                reflectiveQuadTo(560f, 520f)
                quadToRelative(0f, 49f, 22f, 90.5f)
                reflectiveQuadToRelative(58f, 69.5f)
                lineTo(537f, 783f)
                quadToRelative(-12f, 12f, -26.5f, 18f)
                reflectiveQuadToRelative(-30.5f, 6f)
                quadToRelative(-16f, 0f, -30.5f, -6f)
                reflectiveQuadTo(423f, 783f)
                close()
                moveTo(760f, 800f)
                quadToRelative(-17f, 0f, -29.5f, -12.5f)
                reflectiveQuadTo(718f, 758f)
                quadToRelative(0f, -17f, 12.5f, -29.5f)
                reflectiveQuadTo(760f, 716f)
                quadToRelative(17f, 0f, 29.5f, 12.5f)
                reflectiveQuadTo(802f, 758f)
                quadToRelative(0f, 17f, -12.5f, 29.5f)
                reflectiveQuadTo(760f, 800f)
                close()
                moveTo(876f, 503f)
                quadToRelative(0f, 23f, -10f, 41f)
                reflectiveQuadToRelative(-38f, 46f)
                quadToRelative(-17f, 17f, -24.5f, 28f)
                reflectiveQuadToRelative(-9.5f, 25f)
                quadToRelative(-2f, 12f, -11.5f, 20.5f)
                reflectiveQuadTo(761f, 672f)
                quadToRelative(-13f, 0f, -22f, -9f)
                reflectiveQuadToRelative(-7f, -21f)
                quadToRelative(3f, -23f, 14f, -40f)
                reflectiveQuadToRelative(37f, -43f)
                quadToRelative(21f, -21f, 27f, -31.5f)
                reflectiveQuadToRelative(6f, -26.5f)
                quadToRelative(0f, -18f, -14f, -31.5f)
                reflectiveQuadTo(765f, 456f)
                quadToRelative(-15f, 0f, -29f, 7f)
                reflectiveQuadToRelative(-24f, 20f)
                quadToRelative(-7f, 9f, -17.5f, 13f)
                reflectiveQuadToRelative(-21.5f, -1f)
                quadToRelative(-11f, -5f, -16.5f, -15f)
                reflectiveQuadToRelative(0.5f, -20f)
                quadToRelative(16f, -28f, 44.5f, -44f)
                reflectiveQuadToRelative(63.5f, -16f)
                quadToRelative(49f, 0f, 80f, 29f)
                reflectiveQuadToRelative(31f, 74f)
                close()
            }
        }.build()

        return _SignalWifiNotConnected!!
    }

@Suppress("ObjectPropertyName")
private var _SignalWifiNotConnected: ImageVector? = null
