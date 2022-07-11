package io.github.sds100.keymapper.system

import io.github.sds100.keymapper.system.shell.ShellAdapter
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.success
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

/**
 * Created by sds100 on 05/11/2018.
 */
class Shell @Inject constructor() : ShellAdapter {
    /**
     * Remember to close it after using it.
     */
    @Throws(IOException::class)
    override fun getShellCommandStdOut(vararg command: String): InputStream {
        return Runtime.getRuntime().exec(command).inputStream
    }

    /**
     * Remember to close it after using it.
     */
    @Throws(IOException::class)
    fun getShellCommandStdErr(vararg command: String): InputStream {
        return Runtime.getRuntime().exec(command).errorStream
    }

    override fun execute(vararg command: String, waitFor: Boolean): Result<*> {
        try {
            val process = Runtime.getRuntime().exec(command)

            if (waitFor) {
                process.waitFor()
            }

            return success()
        } catch (e: IOException) {
            return Error.Exception(e)
        }
    }
}