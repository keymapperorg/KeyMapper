package io.github.sds100.keymapper.base.actions

import android.os.Build
import io.github.sds100.keymapper.common.models.ShellExecutionMode
import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shell.ShellAdapter
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
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
    ): KMResult<ShellResult> {
        return try {
            withTimeout(timeoutMillis) {
                when (executionMode) {
                    ShellExecutionMode.STANDARD -> shellAdapter.executeWithOutput(command)
                    ShellExecutionMode.ROOT -> suAdapter.executeWithOutput(command)
                    ShellExecutionMode.ADB -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            systemBridgeConnectionManager.run { systemBridge ->
                                systemBridge.executeCommand(command)
                            }
                        } else {
                            KMError.SdkVersionTooLow(Build.VERSION_CODES.Q)
                        }
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            KMError.ShellCommandTimeout(timeoutMillis.toInt())
        }
    }

    fun executeWithStreamingOutput(
        command: String,
        executionMode: ShellExecutionMode,
    ): Flow<KMResult<ShellResult>> {
        return when (executionMode) {
            ShellExecutionMode.STANDARD -> shellAdapter.executeWithStreamingOutput(command)
            ShellExecutionMode.ROOT -> suAdapter.executeWithStreamingOutput(command)

            ShellExecutionMode.ADB -> {
                // ADB mode doesn't support streaming, so we execute synchronously and return a single result
                flow {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val result = systemBridgeConnectionManager.run { systemBridge ->
                            systemBridge.executeCommand(command)
                        }

                        emit(result)
                    } else {
                        emit(KMError.SdkVersionTooLow(Build.VERSION_CODES.Q))
                    }

                }
            }
        }
    }
}

