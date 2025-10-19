package io.github.sds100.keymapper.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the result of a shell command execution.
 * Contains both stdout and stderr output along with exit code information.
 *
 * @param stdOut The stdout output from the command
 * @param stdErr The stderr output from the command
 * @param exitCode The exit code of the command (0 typically means success)
 */
@Parcelize
data class ShellResult(
    val stdOut: String,
    val stdErr: String,
    val exitCode: Int,
) : Parcelable

fun ShellResult.isSuccess(): Boolean = exitCode == 0
fun ShellResult.isError(): Boolean = exitCode != 0
