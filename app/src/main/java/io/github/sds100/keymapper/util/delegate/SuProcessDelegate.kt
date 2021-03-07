package io.github.sds100.keymapper.util.delegate

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.github.sds100.keymapper.util.RootUtils
import timber.log.Timber
import java.io.IOException

/**
 * Created by sds100 on 22/10/20.
 */
class SuProcessDelegate : LifecycleObserver {

    var process: Process? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onCreate() {
        createSuProcess()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun stopSuProcess() {
        process?.destroy()
    }

    fun createSuProcess() {
        try {
            process = RootUtils.getSuProcess()
        } catch (e: IOException) {
            Timber.i("No root $e")
        }
    }
}