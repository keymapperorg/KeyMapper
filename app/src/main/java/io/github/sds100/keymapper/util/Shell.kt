package io.github.sds100.keymapper.util

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


/**
 * Created by sds100 on 05/11/2018.
 */
object Shell {
    /**
     * @return whether the command was executed successfully
     */
    fun run(vararg command: String): Boolean {
        return try {
            Runtime.getRuntime().exec(command)
            true
        } catch (e: IOException) {
            false
        }
    }

    @Throws(IOException::class)
    fun getCommandOutput(vararg command: String): List<String> {
        val process = Runtime.getRuntime().exec(command)

        val bufferedReader = BufferedReader(InputStreamReader(process.errorStream))
        val line = bufferedReader.readLines()

        process.waitFor()

        bufferedReader.close()

        return line
    }
}