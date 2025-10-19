package io.github.sds100.keymapper.system.shell

import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleShell @Inject constructor() : ShellAdapter {

    override fun execute(command: String): KMResult<Unit> {
        try {
            Runtime.getRuntime().exec(prepareCommand(command))

            return Success(Unit)
        } catch (e: IOException) {
            return KMError.Exception(e)
        }
    }

    override fun executeWithOutput(command: String): KMResult<ShellResult> {
        try {
            val process = Runtime.getRuntime().exec(prepareCommand(command))

            process.waitFor()

            val outputLines = with(process.inputStream.bufferedReader()) {
                readLines()
            }
            val errorLines = with(process.errorStream.bufferedReader()) {
                readLines()
            }

            val output = outputLines.joinToString("\n")
            val stderr = errorLines.joinToString("\n")
            val exitCode = process.exitValue()

            return Success(ShellResult(output, stderr, exitCode))
        } catch (e: IOException) {
            return KMError.Exception(e)
        }
    }

    override suspend fun executeWithStreamingOutput(command: String): KMResult<Flow<ShellResult>> {
        return try {
            val process = Runtime.getRuntime().exec(prepareCommand(command))

            val flow = flow {
                val outputReader = process.inputStream.bufferedReader()
                val errorReader = process.errorStream.bufferedReader()

                // Read output line by line
                val stdout = StringBuilder()

                var line: String? = null

                while (outputReader.readLine().also { line = it } != null) {
                    if (line != null) {
                        stdout.appendLine(line)
                    }

                    emit(ShellResult(stdout.toString(), "", 0))
                }

                process.waitFor()

                val stderr = errorReader.readText()
                val exitCode = process.exitValue()

                // Emit final result with both stdout and stderr
                emit(ShellResult(stdout.toString(), stderr, exitCode))

                outputReader.close()
                errorReader.close()
            }.flowOn(Dispatchers.IO)

            Success(flow)
        } catch (e: IOException) {
            KMError.Exception(e)
        }
    }

    private fun prepareCommand(command: String): Array<String> {
        // Execute through sh -c to properly handle multi-line commands and shell syntax
        return arrayOf("sh", "-c", command)
    }
}