package io.github.sds100.keymapper.system

import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.shell.ShellAdapter
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Shell @Inject constructor() : ShellAdapter {
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

    /**
     * Remember to close it after using it.
     */
    @Throws(IOException::class)
    override fun getShellCommandStdOut(vararg command: String): InputStream = Runtime.getRuntime().exec(command).inputStream

    /**
     * Remember to close it after using it.
     */
    @Throws(IOException::class)
    fun getShellCommandStdErr(vararg command: String): InputStream = Runtime.getRuntime().exec(command).errorStream

    override fun execute(command: String): KMResult<*> {
        try {
            Runtime.getRuntime().exec(command)

            return Success(Unit)
        } catch (e: IOException) {
            return KMError.Exception(e)
        }
    }
}
