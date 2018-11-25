package io.github.sds100.keymapper.Utils

import android.content.Context

/**
 * Created by sds100 on 01/10/2018.
 */
object RootUtils {
    private val ROOT_CHECK_COMMAND = arrayOf("su", "-c", "ls")

    /**
     * @return whether the command was executed successfully
     */
    fun executeRootCommand(command: String): Boolean {
        return ShellUtils.executeCommand("su", "-c", command)
    }

    fun checkAppHasRootPermission(): Boolean {
        val output = ShellUtils.getCommandOutput(*ROOT_CHECK_COMMAND)

        if (output.contains("Permission denied")) {
            return false
        } else if (output.isNullOrEmpty()) {
            return true
        }

        return false
    }

    fun promptForRootPermission(ctx: Context) {

    }
}