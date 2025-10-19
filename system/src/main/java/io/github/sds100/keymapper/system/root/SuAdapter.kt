package io.github.sds100.keymapper.system.root

import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.shell.ShellAdapter
import io.github.sds100.keymapper.system.shell.ShellResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
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

            return if (result.isSuccess) {
                Success(ShellResult.Success(output, exitCode))
            } else {
                Success(ShellResult.Error(stderr, exitCode))
            }
        } catch (e: Exception) {
            return KMError.Exception(e)
        }
    }

    override fun executeWithStreamingOutput(command: String): Flow<KMResult<ShellResult>> =
        callbackFlow {
            if (!isRootGranted.firstBlocking()) {
                trySend(SystemError.PermissionDenied(Permission.ROOT))
                close()
                return@callbackFlow
            }

            try {
                val outputLines = mutableListOf<String>()
                val errorLines = mutableListOf<String>()

                Shell.cmd(command)
                    .to(object : CallbackList<String>() {
                        override fun onAddElement(s: String) {
                            outputLines.add(s)
                            trySend(Success(ShellResult.Success(outputLines.joinToString("\n"))))
                        }
                    })
                    .to(errorLines)
                    .submit { result ->
                        val output = outputLines.joinToString("\n")
                        val stderr = errorLines.joinToString("\n")
                        val exitCode = result.code

                        if (result.isSuccess) {
                            trySend(Success(ShellResult.Success(output, exitCode)))
                        } else {
                            trySend(Success(ShellResult.Error(stderr, exitCode)))
                        }
                        close()
                    }

                awaitClose { }
            } catch (e: Exception) {
                trySend(KMError.Exception(e))
                close()
            }
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
