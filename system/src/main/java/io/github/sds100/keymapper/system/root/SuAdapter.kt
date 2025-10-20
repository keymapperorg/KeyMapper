package io.github.sds100.keymapper.system.root

import com.topjohnwu.superuser.Shell
import io.github.sds100.keymapper.system.shell.BaseShellAdapter
import io.github.sds100.keymapper.system.shell.ShellAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuAdapterImpl @Inject constructor() : BaseShellAdapter(), SuAdapter {
    override val isRootGranted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        Shell.getShell()
        invalidateIsRooted()
    }

    override fun requestPermission() {
        invalidateIsRooted()
    }

    override fun prepareCommand(command: String): Array<String> {
        // Execute through su -c to properly handle multi-line commands and shell syntax
        return arrayOf("su", "-c", command)
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
