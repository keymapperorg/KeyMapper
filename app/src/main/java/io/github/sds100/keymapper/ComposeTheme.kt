package io.github.sds100.keymapper

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
        inverseSurface = ComposeColors.inverseSurfaceLight,
        inverseOnSurface = ComposeColors.inverseOnSurfaceLight,
        inversePrimary = ComposeColors.inversePrimaryLight,
    )

    val darkScheme =
        darkColorScheme(
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
            inverseSurface = ComposeColors.inverseSurfaceDark,
            inverseOnSurface = ComposeColors.inverseOnSurfaceDark,
        )
}

@Composable
fun KeyMapperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> ComposeTheme.darkScheme
        else -> ComposeTheme.lightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surfaceContainer.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
