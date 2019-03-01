package io.github.sds100.keymapper.util

/**
 * Created by sds100 on 05/11/2018.
 */

object StatusBarUtils {
    fun expandNotificationDrawer() = Shell.run("cmd", "statusbar", "expand-notifications")
    fun expandQuickSettings() = Shell.run("cmd", "statusbar", "expand-settings")
    fun collapseStatusBar() = Shell.run("cmd", "statusbar", "collapse")
}