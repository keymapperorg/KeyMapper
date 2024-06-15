package io.github.sds100.keymapper.system.shell

import io.github.sds100.keymapper.util.Result

/**
 * Created by sds100 on 21/04/2021.
 */

interface ShellAdapter {
    fun execute(command: String): Result<*>
}
