package io.github.sds100.keymapper.system.root

import com.topjohnwu.superuser.Shell
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuAdapterImpl @Inject constructor(
    coroutineScope: CoroutineScope,
) : SuAdapter {
    private var process: Process? = null

    override val isRooted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        invalidateIsRooted()
    }

    override fun requestPermission(): Boolean {
        // show the su prompt
        Shell.getShell()

        return isRooted.updateAndGet { Shell.isAppGrantedRoot() ?: false }
    }

    override fun execute(command: String, block: Boolean): KMResult<*> {
        if (!isRooted.firstBlocking()) {
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
        Shell.getShell()
        isRooted.update { Shell.isAppGrantedRoot() ?: false }
    }
}

interface SuAdapter {
    val isRooted: StateFlow<Boolean>

    /**
     * @return whether root permission was granted successfully
     */
    fun requestPermission(): Boolean
    fun execute(command: String, block: Boolean = false): KMResult<*>
}
