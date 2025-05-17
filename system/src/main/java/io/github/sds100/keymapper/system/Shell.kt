package io.github.sds100.keymapper.system

import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.shell.ShellAdapter
import java.io.IOException
import java.io.InputStream


object Shell : ShellAdapter {
    /**
     * @return whether the command was executed successfully
     */
    fun run(vararg command: String, waitFor: Boolean = false): Boolean = try {
        val process = Runtime.getRuntime().exec(command)

        if (waitFor) {
            process.waitFor()
        }

        true
    } catch (e: IOException) {
        false
    }

    /**
     * Remember to close it after using it.
     */
    @Throws(IOException::class)
    fun getShellCommandStdOut(vararg command: String): InputStream =
        Runtime.getRuntime().exec(command).inputStream

    /**
     * Remember to close it after using it.
     */
    @Throws(IOException::class)
    fun getShellCommandStdErr(vararg command: String): InputStream =
        Runtime.getRuntime().exec(command).errorStream

    override fun execute(command: String): Result<*> {
        try {
            Runtime.getRuntime().exec(command)

            return Success(Unit)
        } catch (e: IOException) {
            return Error.Exception(e)
        }
    }
}
