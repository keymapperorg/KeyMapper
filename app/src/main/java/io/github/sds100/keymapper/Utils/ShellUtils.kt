package io.github.sds100.keymapper.Utils

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader


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

    fun getCommandOutput(vararg command: String): List<String> {
        try {
            val process = Runtime.getRuntime().exec(command)

            val bufferedReader = BufferedReader(InputStreamReader(process.errorStream))
            val line = bufferedReader.readLines()

            process.waitFor()

            bufferedReader.close()

            return line
        } catch (e: Exception) {
            Log.e(this::class.java.simpleName, e.toString())
            return listOf()
        }
    }
}