package io.github.sds100.keymapper.system.shell

import io.github.sds100.keymapper.util.Result
import java.io.InputStream

/**
 * Created by sds100 on 21/04/2021.
 */

interface ShellAdapter {
    fun execute(vararg command: String, waitFor: Boolean = false): Result<*>
    fun getShellCommandStdOut(vararg command: String): InputStream
}