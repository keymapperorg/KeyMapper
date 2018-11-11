package io.github.sds100.keymapper.Utils

/**
 * Created by sds100 on 01/10/2018.
 */
object RootUtils {
    /**
     * @return whether the command was executed successfully
     */
    fun executeRootCommand(command: String): Boolean {
        return ShellUtils.executeCommand("su", "-c", command)
    }
}