package io.github.sds100.keymapper.util

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

/**
 * Created by sds100 on 01/08/2018.
 */

object AppShortcutUtils {
    fun getAppShortcuts(packageManager: PackageManager): List<ResolveInfo> {
        val shortcutIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)
        return packageManager.queryIntentActivities(shortcutIntent, 0)
    }
}