package io.github.sds100.keymapper.system.root

import io.github.sds100.keymapper.common.util.result.Error
import io.github.sds100.keymapper.common.util.result.Result
import io.github.sds100.keymapper.common.util.result.Success
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.Shell
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.base.util.firstBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import java.io.InputStream



class SuAdapterImpl(
    coroutineScope: CoroutineScope,
    private val preferenceRepository: PreferenceRepository,
) : SuAdapter {
    private var process: Process? = null

    override val isGranted: StateFlow<Boolean> = preferenceRepository.get(Keys.hasRootPermission)
        .map { it ?: false }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override fun requestPermission(): Boolean {
        preferenceRepository.set(Keys.hasRootPermission, true)

        // show the su prompt
        Shell.run("su")

        return true
    }

    override fun execute(command: String, block: Boolean): Result<*> {
        if (!isGranted.firstBlocking()) {
            return SystemError.PermissionDenied(Permission.ROOT)
        }

        try {
            if (block) {
                // Don't use the long running su process because that will block the thread indefinitely
                Shell.run("su", "-c", command, waitFor = true)
            } else {
                if (process == null) {
                    process = ProcessBuilder("su").start()
                }

                with(process!!.outputStream.bufferedWriter()) {
                    write("$command\n")
                    flush()
                }
            }

            return Success(Unit)
        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }

    override fun getCommandOutput(command: String): Result<InputStream> {
        if (!isGranted.firstBlocking()) {
            return SystemError.PermissionDenied(Permission.ROOT)
        }

        try {
            val inputStream = Shell.getShellCommandStdOut("su", "-c", command)
            return Success(inputStream)
        } catch (e: IOException) {
            return Error.UnknownIOError
        }
    }
}

interface SuAdapter {
    val isGranted: StateFlow<Boolean>

    /**
     * @return whether root permission was granted successfully
     */
    fun requestPermission(): Boolean
    fun execute(command: String, block: Boolean = false): Result<*>
    fun getCommandOutput(command: String): Result<InputStream>
}
