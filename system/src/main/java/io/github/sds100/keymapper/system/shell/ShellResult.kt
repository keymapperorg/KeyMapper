package io.github.sds100.keymapper.system.shell

/**
 * Represents the result of a shell command execution.
 * Contains both stdout and stderr output along with exit code information.
 */
sealed class ShellResult {
    abstract val exitCode: Int

    /**
     * Successful shell command execution.
     * @param stdout The stdout output from the command
     * @param exitCode The exit code of the command (0 typically means success)
     */
    data class Success(
        val stdout: String,
        override val exitCode: Int = 0
    ) : ShellResult()

    /**
     * Failed shell command execution.
     * @param stderr The stderr output from the command
     * @param exitCode The exit code of the command (non-zero typically means failure)
     */
    data class Error(
        val stderr: String,
        override val exitCode: Int
    ) : ShellResult()
}
