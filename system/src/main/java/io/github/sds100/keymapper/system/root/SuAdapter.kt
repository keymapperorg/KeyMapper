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
import kotlinx.coroutines.flow.updateAndGet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuAdapterImpl @Inject constructor() : SuAdapter {
    override val isRootGranted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isRootDetected: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        Shell.getShell()
        invalidateIsRooted()
    }

    override fun requestPermission(): Boolean {
        Shell.cmd("su").exec()
        return isRootGranted.updateAndGet { Shell.isAppGrantedRoot() ?: false }
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
        isRootDetected.update { Shell.cmd("su").exec().isSuccess }
        isRootGranted.update { Shell.isAppGrantedRoot() ?: false }
    }
}

interface SuAdapter {
    val isRootGranted: StateFlow<Boolean>
    val isRootDetected: StateFlow<Boolean>

    /**
     * @return whether root permission was granted successfully
     */
    fun requestPermission(): Boolean
    fun execute(command: String, block: Boolean = false): KMResult<*>
}
