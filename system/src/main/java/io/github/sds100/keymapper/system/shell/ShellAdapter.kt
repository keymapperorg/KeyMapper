package io.github.sds100.keymapper.system.shell

import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.Flow

interface ShellAdapter {
    fun execute(command: String): KMResult<Unit>
    fun executeWithOutput(command: String): KMResult<ShellResult>
    suspend fun executeWithStreamingOutput(command: String): KMResult<Flow<ShellResult>>
}
