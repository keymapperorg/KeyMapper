package io.github.sds100.keymapper.base.compose

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
fun SetSystemChrome(statusBarColor: Color, navigationBarColor: Color, isDarkTheme: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current

    if (!view.isInEditMode) {
        // Use keys to ensure it updates when theme changes.
        // LaunchedEffect runs when keys change, and on initial composition.
        // It also runs when restoring back stack if the composable is recreated.
        LaunchedEffect(isDarkTheme, statusBarColor, navigationBarColor) {
            val activity = context.findActivity() ?: return@LaunchedEffect

            activity.enableEdgeToEdge(
                statusBarStyle = if (isDarkTheme) {
                    SystemBarStyle.dark(statusBarColor.toArgb())
                } else {
                    SystemBarStyle.light(statusBarColor.toArgb(), statusBarColor.toArgb())
                },
                navigationBarStyle = if (isDarkTheme) {
                    SystemBarStyle.dark(navigationBarColor.toArgb())
                } else {
                    SystemBarStyle.light(navigationBarColor.toArgb(), navigationBarColor.toArgb())
                },
            )
        }
    }
}

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
