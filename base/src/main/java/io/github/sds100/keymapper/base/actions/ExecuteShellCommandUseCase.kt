package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shell.ShellAdapter
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class ExecuteShellCommandUseCase @Inject constructor(
    private val shellAdapter: ShellAdapter,
    private val suAdapter: SuAdapter,
) {
    suspend fun execute(
        command: String,
        useRoot: Boolean,
        timeoutMs: Long,
    ): KMResult<Unit> {
        return try {
            withTimeout(timeoutMs) {
                if (useRoot) {
                    suAdapter.execute(command)
                } else {
                    shellAdapter.execute(command)
                }
            }
        } catch (e: TimeoutCancellationException) {
            KMError.ShellCommandTimeout(timeoutMs.toInt())
        }
    }

    fun executeWithStreamingOutput(
        command: String,
        useRoot: Boolean,
    ): Flow<KMResult<String>> {
        return if (useRoot) {
            suAdapter.executeWithStreamingOutput(command)
        } else {
            shellAdapter.executeWithStreamingOutput(command)
        }
    }
}

