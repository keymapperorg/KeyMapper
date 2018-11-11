package io.github.sds100.keymapper.Utils

/**
 * Created by sds100 on 05/11/2018.
 */
object ShellUtils {
    /**
     * @return whether the command was executed successfully
     */
    fun executeCommand(vararg command: String): Boolean {
        try {
            Runtime.getRuntime().exec(command)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}