package io.github.sds100.keymapper.system.root

import com.topjohnwu.superuser.Shell
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    override fun execute(command: String, block: Boolean): KMResult<*> {
        if (!isRootGranted.firstBlocking()) {
            return SystemError.PermissionDenied(Permission.ROOT)
        }

        try {
            if (block) {
                Shell.cmd(command).exec()
            } else {
                Shell.cmd(command).submit()
            }

            return Success(Unit)
        } catch (e: Exception) {
            return KMError.Exception(e)
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

interface SuAdapter {
    val isRootGranted: StateFlow<Boolean>

    fun requestPermission()
    fun execute(command: String, block: Boolean = false): KMResult<*>
}
