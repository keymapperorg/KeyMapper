package io.github.sds100.keymapper.system.shell

import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleShell @Inject constructor() : ShellAdapter {
    /**
     * @return whether the command was executed successfully
     */
    override fun run(vararg command: String, waitFor: Boolean): Boolean = try {
        val process = Runtime.getRuntime().exec(command)

        if (waitFor) {
            process.waitFor()
        }

        true
    } catch (e: IOException) {
        false
    }

    override fun execute(command: String): KMResult<*> {
        try {
            Runtime.getRuntime().exec(command)

            return Success(Unit)
        } catch (e: IOException) {
            return KMError.Exception(e)
        }
    }

    override fun executeWithOutput(command: String): KMResult<String> {
        try {
            val process = Runtime.getRuntime().exec(command)

            process.waitFor()

            if (process.exitValue() != 0) {
                val errorLines = with(process.errorStream.bufferedReader()) {
                    readLines()
                }
                return KMError.Exception(IOException(errorLines.joinToString("\n")))
            }

            val lines = with(process.inputStream.bufferedReader()) {
                readLines()
            }

            return Success(lines.joinToString("\n"))
        } catch (e: IOException) {
            return KMError.Exception(e)
        }
    }
}