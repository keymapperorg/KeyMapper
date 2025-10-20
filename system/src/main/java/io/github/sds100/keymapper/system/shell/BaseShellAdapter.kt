package io.github.sds100.keymapper.system.shell

import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.InterruptedIOException

abstract class BaseShellAdapter() : ShellAdapter {
    abstract fun prepareCommand(command: String): Array<String>

    override suspend fun execute(
        command: String,
        timeoutMillis: Long,
    ): KMResult<ShellResult> = coroutineScope {
        try {
            val process = ProcessBuilder()
                .command(*prepareCommand(command))
                // Redirect stderr to stdout
                .redirectErrorStream(true)
                .start()

            val stdoutReader = process.inputStream.bufferedReader()
            var stdout = ""

            try {
                val readStdoutJob = launch(Dispatchers.IO) {
                    stdout = stdoutReader.readText()
                }

                val exitCode = withTimeoutOrNull(timeoutMillis) {
                    // This is required so that the blocking process code is interrupted when
                    // the coroutine is cancelled by the timeout.
                    runInterruptible(Dispatchers.IO) {
                        process.waitFor()
                    }
                }

                readStdoutJob.cancel()

                if (exitCode == null) {
                    KMError.ShellCommandTimeout(timeoutMillis, stdout)
                } else {
                    Success(ShellResult(stdout, exitCode))
                }
            } finally {
                stdoutReader.close()
                process.destroy()
            }
        } catch (e: IOException) {
            KMError.Exception(e)
        }
    }

    override suspend fun executeWithStreamingOutput(
        command: String,
        timeoutMillis: Long
    ): Flow<KMResult<ShellResult>> = callbackFlow {
        try {
            val process = ProcessBuilder()
                .command(*prepareCommand(command))
                // Redirect stderr to stdout
                .redirectErrorStream(true)
                .start()

            val stdoutReader = process.inputStream.bufferedReader()
            val stdout = StringBuilder()

            try {
                val readStdoutJob = launch(Dispatchers.IO) {
                    var line: String? = null

                    try {
                        while (stdoutReader.readLine().also { line = it } != null) {
                            stdout.appendLine(line)
                            if (line != null) {
                                send(ShellResult(stdout.toString(), null).success())
                            }
                        }
                    } catch (e: InterruptedIOException) {
                        // Do nothing. This is thrown due to the timeout below.
                    }
                }

                val exitCode = withTimeoutOrNull(timeoutMillis) {
                    // This is required so that the blocking process code is interrupted when
                    // the coroutine is cancelled by the timeout.
                    runInterruptible(Dispatchers.IO) {
                        process.waitFor()
                    }
                }

                readStdoutJob.cancel()

                if (exitCode == null) {
                    send(KMError.ShellCommandTimeout(timeoutMillis, stdout.toString()))
                } else {
                    send(ShellResult(stdout.toString(), exitCode).success())
                }

                readStdoutJob.cancel()

            } finally {
                process.destroy()
                stdoutReader.close()
            }

        } catch (e: IOException) {
            trySend(KMError.Exception(e))
        } finally {
            this@callbackFlow.close()
            awaitClose {}
        }
    }
}