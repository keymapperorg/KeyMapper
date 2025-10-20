package io.github.sds100.keymapper.system.shell

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StandardShellAdapter @Inject constructor() : BaseShellAdapter() {
    override fun prepareCommand(command: String): Array<String> {
        // Execute through sh -c to properly handle multi-line commands and shell syntax
        return arrayOf("sh", "-c", command)
    }
}