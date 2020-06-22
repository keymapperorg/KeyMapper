package io.github.sds100.keymapper.util

import java.io.IOException
import java.io.InputStream


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

    /**
     * Remember to close it after using it.
     */
    fun getShellCommandStdOut(vararg command: String): InputStream {
        return Runtime.getRuntime().exec(command).inputStream
    }

    /**
     * Remember to close it after using it.
     */
    fun getShellCommandStdErr(vararg command: String): InputStream {
        return Runtime.getRuntime().exec(command).errorStream
    }
}