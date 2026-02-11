package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.common.models.ShellExecutionMode
import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shell.ShellAdapter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

class ExecuteShellCommandUseCase @Inject constructor(
    private val shellAdapter: ShellAdapter,
    private val suAdapter: SuAdapter,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
) {
    suspend fun execute(
        command: String,
        executionMode: ShellExecutionMode,
        timeoutMillis: Long,
    ): KMResult<ShellResult> = withContext(Dispatchers.IO) {
        when (executionMode) {
            ShellExecutionMode.STANDARD -> shellAdapter.execute(command, timeoutMillis)
            ShellExecutionMode.ROOT -> suAdapter.execute(command, timeoutMillis)
            ShellExecutionMode.ADB -> executeCommandSystemBridge(command, timeoutMillis)
        }
    }

    suspend fun executeWithStreamingOutput(
        command: String,
        executionMode: ShellExecutionMode,
        timeoutMillis: Long,
    ): Flow<KMResult<ShellResult>> {
        return when (executionMode) {
            ShellExecutionMode.STANDARD -> shellAdapter.executeWithStreamingOutput(
                command,
                timeoutMillis,
            )

            ShellExecutionMode.ROOT -> suAdapter.executeWithStreamingOutput(command, timeoutMillis)

            // ADB mode doesn't support streaming
            ShellExecutionMode.ADB -> flowOf(executeCommandSystemBridge(command, timeoutMillis))
        }
    }

    /**
     * Useful shell command for testing this is:
     * for i in 1 2 3 4 5 6; do sleep 1; echo $i; done
     */
    private suspend fun executeCommandSystemBridge(
        command: String,
        timeoutMillis: Long,
    ): KMResult<ShellResult> {
        return runInterruptible(Dispatchers.IO) {
            try {
                systemBridgeConnectionManager.run { systemBridge ->
                    systemBridge.executeCommand(command, timeoutMillis)
                }
                // Only some standard exceptions can be thrown across Binder.
            } catch (e: IllegalStateException) {
                KMError.ShellCommandTimeout(timeoutMillis, null)
            }
        }
    }
}
