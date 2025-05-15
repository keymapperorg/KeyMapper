package io.github.sds100.keymapper.system.shell

import io.github.sds100.keymapper.common.utils.KMResult

interface ShellAdapter {
    fun run(vararg command: String, waitFor: Boolean = false): Boolean
    fun execute(command: String): KMResult<*>
}
