package io.github.sds100.keymapper.system.shell

import io.github.sds100.keymapper.common.utils.Result



interface ShellAdapter {
    fun execute(command: String): Result<*>
}
