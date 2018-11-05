package io.github.sds100.keymapper.Utils

/**
 * Created by sds100 on 05/11/2018.
 */

object StatusBarUtils {
    fun expandNotificationDrawer() = ShellUtils.executeCommand("cmd", "statusbar", "expand-notifications")
    fun expandQuickSettings() = ShellUtils.executeCommand("cmd", "statusbar", "expand-settings")
    fun collapseStatusBar() = ShellUtils.executeCommand("cmd", "statusbar", "collapse")
}