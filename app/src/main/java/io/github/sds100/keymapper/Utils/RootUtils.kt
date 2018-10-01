package io.github.sds100.keymapper.Utils

/**
 * Created by sds100 on 01/10/2018.
 */
object RootUtils {
    fun executeRootCommand(command: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        } catch (e: Exception) {
        }
    }
}