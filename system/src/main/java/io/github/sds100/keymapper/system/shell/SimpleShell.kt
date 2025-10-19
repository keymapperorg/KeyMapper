package io.github.sds100.keymapper.system.shell

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

            return if (exitCode == 0) {
                Success(ShellResult.Success(output, exitCode))
            } else {
                Success(ShellResult.Error(stderr, exitCode))
            }
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
                    emit(Success(ShellResult.Success(outputLines.joinToString("\n"))))
                }

                process.waitFor()

                // Read stderr after process completes
                val errorLines = errorReader.readLines()
                val stderr = errorLines.joinToString("\n")
                val exitCode = process.exitValue()

                // Emit final result based on exit code
                if (exitCode == 0) {
                    emit(Success(ShellResult.Success(outputLines.joinToString("\n"), exitCode)))
                } else {
                    emit(Success(ShellResult.Error(stderr, exitCode)))
                }

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