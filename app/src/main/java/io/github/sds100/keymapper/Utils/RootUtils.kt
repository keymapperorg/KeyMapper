package io.github.sds100.keymapper.Utils

/**
 * Created by sds100 on 01/10/2018.
 */
object RootUtils {
    /**
     * @return whether the command was executed successfully
     */
    fun executeRootCommand(command: String): Boolean {
        try {
            ShellUtils.executeCommand("su", "-c", command)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun isRooted() = executeRootCommand("ls")
}