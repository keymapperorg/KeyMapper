package io.github.sds100.keymapper.system.shell

import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.Flow

interface ShellAdapter {
    suspend fun execute(
        command: String,
        timeoutMillis: Long = 10000L
    ): KMResult<ShellResult>

    suspend fun executeWithStreamingOutput(
        command: String,
        timeoutMillis: Long
    ): KMResult<Flow<ShellResult>>
}
