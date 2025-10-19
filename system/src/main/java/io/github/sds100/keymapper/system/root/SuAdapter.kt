package io.github.sds100.keymapper.system.root

import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import io.github.sds100.keymapper.common.models.ShellResult
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.shell.ShellAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuAdapterImpl @Inject constructor() : SuAdapter {
    override val isRootGranted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        Shell.getShell()
        invalidateIsRooted()
    }

    override fun requestPermission() {
        invalidateIsRooted()
    }

    override fun execute(command: String): KMResult<Unit> {
        if (!isRootGranted.firstBlocking()) {
            return SystemError.PermissionDenied(Permission.ROOT)
        }

        try {
            Shell.cmd(command).exec()

            return Success(Unit)
        } catch (e: Exception) {
            return KMError.Exception(e)
        }
    }

    override fun executeWithOutput(command: String): KMResult<ShellResult> {
        if (!isRootGranted.firstBlocking()) {
            return SystemError.PermissionDenied(Permission.ROOT)
        }

        try {
            val result = Shell.cmd(command).exec()

            val output = result.out.joinToString("\n")
            val stderr = result.err.joinToString("\n")
            val exitCode = result.code

            return Success(ShellResult(output, stderr, exitCode))
        } catch (e: Exception) {
            return KMError.Exception(e)
        }
    }

    override suspend fun executeWithStreamingOutput(command: String): KMResult<Flow<ShellResult>> {
        if (!isRootGranted.firstBlocking()) {
            return SystemError.PermissionDenied(Permission.ROOT)
        }

        val flow = callbackFlow {
            val stdout = StringBuilder()
            val stderr = StringBuilder()

            val outputCallback = object : CallbackList<String>() {
                override fun onAddElement(s: String) {
                    stdout.appendLine(s)

                    trySendBlocking(
                        ShellResult(
                            stdout.toString(),
                            stderr.toString(),
                            0
                        )
                    )
                }
            }

            val errorCallback = object : CallbackList<String>() {
                override fun onAddElement(s: String) {
                    stderr.appendLine(s)

                    trySendBlocking(
                        ShellResult(
                            stdout.toString(),
                            stderr.toString(),
                            0
                        )
                    )
                }
            }

            Shell.cmd(command)
                .to(outputCallback, errorCallback)
                .submit { result ->
                    trySendBlocking(
                        ShellResult(
                            stdout.toString(),
                            stderr.toString(),
                            result.code
                        )
                    )
                    close()
                }

            awaitClose { }
        }.flowOn(Dispatchers.IO)

        return Success(flow)
    }

    fun invalidateIsRooted() {
        try {
            // Close the shell so a new one is started without root permission.
            Shell.getShell().waitAndClose()
            val isRooted = Shell.isAppGrantedRoot() ?: false
            isRootGranted.update { isRooted }

            if (isRooted) {
                Timber.i("Root access granted")
            } else {
                Timber.i("Root access denied")
            }
        } catch (e: Exception) {
            Timber.e("Exception invalidating root detection: $e")
        }
    }
}

interface SuAdapter : ShellAdapter {
    val isRootGranted: StateFlow<Boolean>

    fun requestPermission()
}
