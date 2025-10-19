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

    override fun executeWithStreamingOutput(command: String): Flow<KMResult<ShellResult>> {
        return flow {
            try {
                val process = Runtime.getRuntime().exec(prepareCommand(command))
                val outputReader = process.inputStream.bufferedReader()
                val errorReader = process.errorStream.bufferedReader()
                val outputLines = mutableListOf<String>()

                // Read output line by line
                var line: String?
                while (outputReader.readLine().also { line = it } != null) {
                    outputLines.add(line!!)
                    emit(Success(ShellResult(outputLines.joinToString("\n"), "", 0)))
                }

                process.waitFor()

                // Read stderr after process completes
                val errorLines = errorReader.readLines()
                val stderr = errorLines.joinToString("\n")
                val exitCode = process.exitValue()

                // Emit final result with both stdout and stderr
                emit(Success(ShellResult(outputLines.joinToString("\n"), stderr, exitCode)))

                outputReader.close()
                errorReader.close()
            } catch (e: IOException) {
                emit(KMError.Exception(e))
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun prepareCommand(command: String): Array<String> {
        // Execute through sh -c to properly handle multi-line commands and shell syntax
        return arrayOf("sh", "-c", command)
    }
}