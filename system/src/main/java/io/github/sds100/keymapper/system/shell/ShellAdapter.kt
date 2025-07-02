package io.github.sds100.keymapper.system.shell

import io.github.sds100.keymapper.common.utils.KMResult
import java.io.InputStream

interface ShellAdapter {
    fun run(vararg command: String, waitFor: Boolean = false): Boolean
    fun execute(command: String): KMResult<*>
    fun getShellCommandStdOut(vararg command: String): InputStream
}
