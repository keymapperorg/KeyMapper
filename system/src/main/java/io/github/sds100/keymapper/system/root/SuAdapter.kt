package io.github.sds100.keymapper.system.root

import com.topjohnwu.superuser.Shell
import io.github.sds100.keymapper.system.shell.BaseShellAdapter
import io.github.sds100.keymapper.system.shell.ShellAdapter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class SuAdapterImpl @Inject constructor(private val coroutineScope: CoroutineScope) :
    BaseShellAdapter(),
    SuAdapter {
    override val isRootGranted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var invalidateJob: Job? = null

    init {
        invalidateJob?.cancel()
        invalidateJob = coroutineScope.launch {
            invalidateIsRooted()
        }
    }

    override fun requestPermission() {
        invalidateJob?.cancel()
        invalidateJob = coroutineScope.launch {
            invalidateIsRooted()
        }
    }

    override fun prepareCommand(command: String): Array<String> {
        // Execute through su -c to properly handle multi-line commands and shell syntax
        return arrayOf("su", "-c", command)
    }

    private suspend fun invalidateIsRooted() {
        try {
            // Close the shell so a new one is started without root permission.
            val isRooted = getIsRooted()
            isRootGranted.update { isRooted }
        } catch (e: Exception) {
            Timber.e("Exception invalidating root detection: $e")
        }
    }

    // Must execute on a separate thread so it doesn't block the Main thread.
    private suspend fun getIsRooted(): Boolean {
        return withContext(Dispatchers.IO) {
            Shell.getShell().waitAndClose()
            Shell.isAppGrantedRoot() ?: false
        }
    }
}

interface SuAdapter : ShellAdapter {
    val isRootGranted: StateFlow<Boolean>

    fun requestPermission()
}
