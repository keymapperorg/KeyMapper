package io.github.sds100.keymapper.system.apps

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.core.content.pm.ShortcutInfoCompat
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.flow.Flow

interface AppShortcutAdapter {
    val installedAppShortcuts: Flow<State<List<AppShortcutInfo>>>
    val areLauncherShortcutsSupported: Boolean

    fun createLauncherShortcut(
        icon: Drawable,
        label: String,
        intentAction: String,
        intentExtras: Bundle,
    ): ShortcutInfoCompat

    fun createLauncherShortcut(
        iconResId: Int,
        label: String,
        intentAction: String,
        intentExtras: Bundle,
    ): ShortcutInfoCompat

    fun pinShortcut(shortcut: ShortcutInfoCompat): KMResult<*>
    fun createShortcutResultIntent(shortcut: ShortcutInfoCompat): Intent

    fun getShortcutName(info: AppShortcutInfo): KMResult<String>
    fun getShortcutIcon(info: AppShortcutInfo): KMResult<Drawable>
    fun launchShortcut(uri: String): KMResult<*>
}
