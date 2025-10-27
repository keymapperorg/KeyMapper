package io.github.sds100.keymapper.base.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

object ComposeTheme {

    val lightScheme = lightColorScheme(
        primary = ComposeColors.primaryLight,
        onPrimary = ComposeColors.onPrimaryLight,
        primaryContainer = ComposeColors.primaryContainerLight,
        onPrimaryContainer = ComposeColors.onPrimaryContainerLight,
        secondary = ComposeColors.secondaryLight,
        onSecondary = ComposeColors.onSecondaryLight,
        secondaryContainer = ComposeColors.secondaryContainerLight,
        onSecondaryContainer = ComposeColors.onSecondaryContainerLight,
        tertiary = ComposeColors.tertiaryLight,
        onTertiary = ComposeColors.onTertiaryLight,
        tertiaryContainer = ComposeColors.tertiaryContainerLight,
        onTertiaryContainer = ComposeColors.onTertiaryContainerLight,
        error = ComposeColors.errorLight,
        onError = ComposeColors.onErrorLight,
        errorContainer = ComposeColors.errorContainerLight,
        onErrorContainer = ComposeColors.onErrorContainerLight,
        background = ComposeColors.backgroundLight,
        onBackground = ComposeColors.onBackgroundLight,
        surface = ComposeColors.surfaceLight,
        onSurface = ComposeColors.onSurfaceLight,
        surfaceVariant = ComposeColors.surfaceVariantLight,
        onSurfaceVariant = ComposeColors.onSurfaceVariantLight,
        outline = ComposeColors.outlineLight,
        outlineVariant = ComposeColors.outlineVariantLight,
        scrim = ComposeColors.scrimLight,
        inverseSurface = ComposeColors.inverseSurfaceLight,
        inverseOnSurface = ComposeColors.inverseOnSurfaceLight,
        inversePrimary = ComposeColors.inversePrimaryLight,
        surfaceDim = ComposeColors.surfaceDimLight,
        surfaceBright = ComposeColors.surfaceBrightLight,
        surfaceContainerLowest = ComposeColors.surfaceContainerLowestLight,
        surfaceContainerLow = ComposeColors.surfaceContainerLowLight,
        surfaceContainer = ComposeColors.surfaceContainerLight,
        surfaceContainerHigh = ComposeColors.surfaceContainerHighLight,
        surfaceContainerHighest = ComposeColors.surfaceContainerHighestLight,
    )

    val darkScheme = darkColorScheme(
        primary = ComposeColors.primaryDark,
        onPrimary = ComposeColors.onPrimaryDark,
        primaryContainer = ComposeColors.primaryContainerDark,
        onPrimaryContainer = ComposeColors.onPrimaryContainerDark,
        secondary = ComposeColors.secondaryDark,
        onSecondary = ComposeColors.onSecondaryDark,
        secondaryContainer = ComposeColors.secondaryContainerDark,
        onSecondaryContainer = ComposeColors.onSecondaryContainerDark,
        tertiary = ComposeColors.tertiaryDark,
        onTertiary = ComposeColors.onTertiaryDark,
        tertiaryContainer = ComposeColors.tertiaryContainerDark,
        onTertiaryContainer = ComposeColors.onTertiaryContainerDark,
        error = ComposeColors.errorDark,
        onError = ComposeColors.onErrorDark,
        errorContainer = ComposeColors.errorContainerDark,
        onErrorContainer = ComposeColors.onErrorContainerDark,
        background = ComposeColors.backgroundDark,
        onBackground = ComposeColors.onBackgroundDark,
        surface = ComposeColors.surfaceDark,
        onSurface = ComposeColors.onSurfaceDark,
        surfaceVariant = ComposeColors.surfaceVariantDark,
        onSurfaceVariant = ComposeColors.onSurfaceVariantDark,
        outline = ComposeColors.outlineDark,
        outlineVariant = ComposeColors.outlineVariantDark,
        scrim = ComposeColors.scrimDark,
        inverseSurface = ComposeColors.inverseSurfaceDark,
        inverseOnSurface = ComposeColors.inverseOnSurfaceDark,
        inversePrimary = ComposeColors.inversePrimaryDark,
        surfaceDim = ComposeColors.surfaceDimDark,
        surfaceBright = ComposeColors.surfaceBrightDark,
        surfaceContainerLowest = ComposeColors.surfaceContainerLowestDark,
        surfaceContainerLow = ComposeColors.surfaceContainerLowDark,
        surfaceContainer = ComposeColors.surfaceContainerDark,
        surfaceContainerHigh = ComposeColors.surfaceContainerHighDark,
        surfaceContainerHighest = ComposeColors.surfaceContainerHighestDark,
    )
}

val LocalCustomColorsPalette = staticCompositionLocalOf { ComposeCustomColors() }

@Composable
fun KeyMapperTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
//    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
//        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
//        dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> ComposeTheme.darkScheme
        else -> ComposeTheme.lightScheme
    }

    val customColorsPalette =
        if (darkTheme) ComposeCustomColors.DarkPalette else ComposeCustomColors.LightPalette

    CompositionLocalProvider(
        LocalCustomColorsPalette provides customColorsPalette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content,
        )
    }
}
