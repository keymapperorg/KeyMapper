package io.github.sds100.keymapper.system.shell

import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.Flow

interface ShellAdapter {
    fun execute(command: String): KMResult<Unit>
    fun executeWithOutput(command: String): KMResult<ShellResult>
    fun executeWithStreamingOutput(command: String): Flow<KMResult<ShellResult>>
}
