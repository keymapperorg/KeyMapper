package io.github.sds100.keymapper.base.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import io.github.sds100.keymapper.base.compose.ComposeColors.onOrangeContainerDark
import io.github.sds100.keymapper.base.compose.ComposeColors.onOrangeContainerLight
import io.github.sds100.keymapper.base.compose.ComposeColors.onOrangeDark
import io.github.sds100.keymapper.base.compose.ComposeColors.onOrangeLight
import io.github.sds100.keymapper.base.compose.ComposeColors.orangeContainerDark
import io.github.sds100.keymapper.base.compose.ComposeColors.orangeContainerLight
import io.github.sds100.keymapper.base.compose.ComposeColors.orangeDark
import io.github.sds100.keymapper.base.compose.ComposeColors.orangeLight

/**
 * Stores the custom colors in a palette that changes
 * depending on the light/dark theme. A CompositionLocalProvider
 * is used in the KeyMapperTheme to provide the correct palette in a similar
 * way to how MaterialTheme.current works.
 */
@Immutable
data class ComposeCustomColors(
    val red: Color = Color.Unspecified,
    val onRed: Color = Color.Unspecified,
    val green: Color = Color.Unspecified,
    val onGreen: Color = Color.Unspecified,
    val greenContainer: Color = Color.Unspecified,
    val onGreenContainer: Color = Color.Unspecified,
    val magiskTeal: Color = Color.Unspecified,
    val onMagiskTeal: Color = Color.Unspecified,
    val shizukuBlue: Color = Color.Unspecified,
    val onShizukuBlue: Color = Color.Unspecified,
    val orange: Color = Color.Unspecified,
    val onOrange: Color = Color.Unspecified,
    val orangeContainer: Color = Color.Unspecified,
    val onOrangeContainer: Color = Color.Unspecified,
) {
    companion object {
        val LightPalette = ComposeCustomColors(
            red = ComposeColors.redLight,
            onRed = ComposeColors.onRedLight,
            green = ComposeColors.greenLight,
            onGreen = ComposeColors.onGreenLight,
            greenContainer = ComposeColors.greenContainerLight,
            onGreenContainer = ComposeColors.onGreenContainerLight,
            magiskTeal = ComposeColors.magiskTealLight,
            onMagiskTeal = ComposeColors.onMagiskTealLight,
            shizukuBlue = ComposeColors.shizukuBlueLight,
            onShizukuBlue = ComposeColors.onShizukuBlueLight,
            orange = orangeLight,
            onOrange = onOrangeLight,
            orangeContainer = orangeContainerLight,
            onOrangeContainer = onOrangeContainerLight,
        )

        val DarkPalette = ComposeCustomColors(
            red = ComposeColors.redDark,
            onRed = ComposeColors.onRedDark,
            green = ComposeColors.greenDark,
            onGreen = ComposeColors.onGreenDark,
            greenContainer = ComposeColors.greenContainerDark,
            onGreenContainer = ComposeColors.onGreenContainerDark,
            magiskTeal = ComposeColors.magiskTealDark,
            onMagiskTeal = ComposeColors.onMagiskTealDark,
            shizukuBlue = ComposeColors.shizukuBlueDark,
            onShizukuBlue = ComposeColors.onShizukuBlueDark,
            orange = orangeDark,
            onOrange = onOrangeDark,
            orangeContainer = orangeContainerDark,
            onOrangeContainer = onOrangeContainerDark,
        )
    }

    @Composable
    @Stable
    fun contentColorFor(color: Color): Color {
        return when (color) {
            red -> onRed
            green -> onGreen
            greenContainer -> onGreenContainer
            magiskTeal -> onMagiskTeal
            shizukuBlue -> onShizukuBlue
            else -> MaterialTheme.colorScheme.contentColorFor(color)
        }
    }
}
