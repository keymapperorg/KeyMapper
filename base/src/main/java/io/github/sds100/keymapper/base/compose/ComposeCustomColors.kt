package io.github.sds100.keymapper.base.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

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
) {
    companion object {
        val LightPalette = ComposeCustomColors(
            red = ComposeColors.redLight,
            onRed = ComposeColors.onRedLight,
            green = ComposeColors.greenLight,
            onGreen = ComposeColors.onGreenLight,
            greenContainer = ComposeColors.greenContainerLight,
            onGreenContainer = ComposeColors.onGreenContainerLight,
        )

        val DarkPalette = ComposeCustomColors(
            red = ComposeColors.redDark,
            onRed = ComposeColors.onRedDark,
            green = ComposeColors.greenDark,
            onGreen = ComposeColors.onGreenDark,
            greenContainer = ComposeColors.greenContainerDark,
            onGreenContainer = ComposeColors.onGreenContainerDark,
        )
    }
}
