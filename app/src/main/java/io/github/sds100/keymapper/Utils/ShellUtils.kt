package io.github.sds100.keymapper.Utils

/**
 * Created by sds100 on 05/11/2018.
 */
object ShellUtils {
    fun executeCommand(vararg command: String) {
        Runtime.getRuntime().exec(command)
    }
}