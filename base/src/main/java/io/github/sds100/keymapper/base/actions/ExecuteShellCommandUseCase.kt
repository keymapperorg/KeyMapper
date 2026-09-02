package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.common.models.ShellExecutionMode
import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.normalizeLineEndings
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shell.ShellAdapter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

/**
 * Line endings are normalized before executing because a shell does not recognize reserved words
 * like "then" when they are followed by a \r, so scripts pasted with Windows line endings fail to
 * parse. See issue #2209.
 */
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
        val sanitizedCommand = command.normalizeLineEndings()

        when (executionMode) {
            ShellExecutionMode.STANDARD -> shellAdapter.execute(sanitizedCommand, timeoutMillis)
            ShellExecutionMode.ROOT -> suAdapter.execute(sanitizedCommand, timeoutMillis)
            ShellExecutionMode.ADB -> executeCommandSystemBridge(sanitizedCommand, timeoutMillis)
        }
    }

    suspend fun executeWithStreamingOutput(
        command: String,
        executionMode: ShellExecutionMode,
        timeoutMillis: Long,
    ): Flow<KMResult<ShellResult>> {
        val sanitizedCommand = command.normalizeLineEndings()

        return when (executionMode) {
            ShellExecutionMode.STANDARD -> shellAdapter.executeWithStreamingOutput(
                sanitizedCommand,
                timeoutMillis,
            )

            ShellExecutionMode.ROOT -> suAdapter.executeWithStreamingOutput(
                sanitizedCommand,
                timeoutMillis,
            )

            // ADB mode doesn't support streaming
            ShellExecutionMode.ADB -> flowOf(
                executeCommandSystemBridge(sanitizedCommand, timeoutMillis),
            )
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
