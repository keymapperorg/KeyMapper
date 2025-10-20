package io.github.sds100.keymapper.system.shell

import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeoutException

abstract class BaseShellAdapter() : ShellAdapter {
    abstract fun prepareCommand(command: String): Array<String>

    override suspend fun execute(
        command: String,
        timeoutMillis: Long,
    ): KMResult<ShellResult> {
        val process = ProcessBuilder()
            .command(*prepareCommand(command))
            // Redirect stderr to stdout
            .redirectErrorStream(true)
            .start()

        val stdoutReader = process.inputStream.bufferedReader()

        try {
            val stdout = withContext(Dispatchers.IO) { stdoutReader.readText() }

            val exitCode: Int? = withTimeoutOrNull(timeoutMillis) {
                withContext(Dispatchers.IO) {
                    runInterruptible { process.waitFor() }
                }
            }

            if (exitCode == null) {
                return KMError.ShellCommandTimeout(timeoutMillis)
            } else {
                return Success(ShellResult(stdout, exitCode))
            }

        } catch (e: IOException) {
            return KMError.Exception(e)
        } finally {
            stdoutReader.close()
            process.destroy()
        }
    }

    override suspend fun executeWithStreamingOutput(
        command: String,
        timeoutMillis: Long
    ): KMResult<Flow<ShellResult>> {
        try {
            Timber.e("Executing command with streaming output: $command")
            val process = ProcessBuilder()
                .command(*prepareCommand(command))
                // Redirect stderr to stdout
                .redirectErrorStream(true)
                .start()

            val stdoutReader = process.inputStream.bufferedReader()

            val flow = callbackFlow {
                try {
                    val readStdoutJob = launch(Dispatchers.IO) {
                        var line: String? = null

                        while (stdoutReader.readLine().also { line = it } != null) {
                            if (line != null) {
                                send(ShellResult(line, null))
                            }
                        }
                    }

                    val exitCode = withTimeoutOrNull(timeoutMillis) {
                        // This is required so that the blocking process code is interrupted when
                        // the coroutine is cancelled by the timeout.
                        runInterruptible(Dispatchers.IO) {
                            process.waitFor()
                        }
                    }

                    if (exitCode == null) {
                        throw TimeoutException()
                    } else {
                        send(ShellResult(command, exitCode))
                    }

                    readStdoutJob.cancel()
                    process.destroy()
                    this@callbackFlow.close()
                    awaitClose {}
                } finally {
                    process.destroy()
                    stdoutReader.close()
                }
            }

            return Success(flow)
        } catch (e: IOException) {
            return KMError.Exception(e)
        }
    }
}