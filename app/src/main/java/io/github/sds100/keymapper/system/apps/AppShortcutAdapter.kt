package io.github.sds100.keymapper.system.apps

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.core.content.pm.ShortcutInfoCompat
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 20/03/2021.
 */

interface AppShortcutAdapter {
    val installedAppShortcuts: Flow<State<List<AppShortcutInfo>>>
    val areLauncherShortcutsSupported: Boolean

    fun createLauncherShortcut(
        icon: Drawable,
        label: String,
        intentAction: String,
        intentExtras: Bundle
    ): ShortcutInfoCompat

    fun pinShortcut(shortcut: ShortcutInfoCompat): Result<*>
    fun createShortcutResultIntent(shortcut: ShortcutInfoCompat): Intent

    fun getShortcutName(info: AppShortcutInfo): Result<String>
    fun getShortcutIcon(info: AppShortcutInfo): Result<Drawable>
    fun launchShortcut(uri: String): Result<*>
}