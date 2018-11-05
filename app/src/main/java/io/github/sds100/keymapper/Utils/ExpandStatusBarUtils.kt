package io.github.sds100.keymapper.Utils

/**
 * Created by sds100 on 05/11/2018.
 */

object ExpandStatusBarUtils {
    fun expandNotificationDrawer() = ShellUtils.executeCommand("cmd", "statusbar", "expand-notifications")
}