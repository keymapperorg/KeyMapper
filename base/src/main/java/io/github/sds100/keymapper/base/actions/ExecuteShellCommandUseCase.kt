package io.github.sds100.keymapper.base.actions

import android.os.Build
import io.github.sds100.keymapper.common.models.ShellExecutionMode
import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shell.ShellAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import javax.inject.Inject

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
            ShellExecutionMode.ADB -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    systemBridgeConnectionManager.run { systemBridge ->
                        systemBridge.executeCommand(command, timeoutMillis)
                    }
                } else {
                    KMError.SdkVersionTooLow(Build.VERSION_CODES.Q)
                }
            }
        }
    }

    suspend fun executeWithStreamingOutput(
        command: String,
        executionMode: ShellExecutionMode,
        timeoutMillis: Long,
    ): KMResult<Flow<ShellResult>> {
        return when (executionMode) {
            ShellExecutionMode.STANDARD -> shellAdapter.executeWithStreamingOutput(
                command,
                timeoutMillis
            )

            ShellExecutionMode.ROOT -> suAdapter.executeWithStreamingOutput(command, timeoutMillis)

            ShellExecutionMode.ADB -> {
                // ADB mode doesn't support streaming
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val result = withContext(Dispatchers.IO) {
                        systemBridgeConnectionManager.run { systemBridge ->
                            systemBridge.executeCommand(command, timeoutMillis)
                        }
                    }
                    when (result) {
                        is KMError -> result
                        is Success -> Success(flowOf(result.value))
                    }
                } else {
                    KMError.SdkVersionTooLow(Build.VERSION_CODES.Q)
                }
            }
        }
    }
}
