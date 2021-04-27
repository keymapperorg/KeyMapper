package io.github.sds100.keymapper.system.statusbar

import io.github.sds100.keymapper.system.Shell

/**
 * Created by sds100 on 05/11/2018.
 */

object StatusBarUtils {
    fun toggleNotificationDrawer(currentPackageName: String) {
        if (currentPackageName == "com.android.systemui") {
            collapseStatusBar()
        } else {
            expandNotificationDrawer()
        }
    }

    fun toggleQuickSettingsDrawer(currentPackageName: String) {
        if (currentPackageName == "com.android.systemui") {
            collapseStatusBar()
        } else {
            expandQuickSettings()
        }
    }

    fun expandNotificationDrawer() = Shell.run("cmd", "statusbar", "expand-notifications")
    fun expandQuickSettings() = Shell.run("cmd", "statusbar", "expand-settings")
    fun collapseStatusBar() = Shell.run("cmd", "statusbar", "collapse")
}