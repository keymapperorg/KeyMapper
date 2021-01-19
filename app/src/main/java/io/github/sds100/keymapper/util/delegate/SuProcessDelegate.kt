package io.github.sds100.keymapper.util.delegate

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.github.sds100.keymapper.util.RootUtils
import splitties.toast.toast
import timber.log.Timber
import java.io.IOException

/**
 * Created by sds100 on 22/10/20.
 */
class SuProcessDelegate : LifecycleObserver {

    private var process: Process? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onCreate() {
        createSuProcess()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun stopSuProcess() {
        process?.destroy()
    }

    private fun createSuProcess() {
        try {
            process = RootUtils.getSuProcess()
        } catch (e: IOException) {
            Timber.i("No root $e")
        }
    }

    fun runCommand(command: String) {
        //the \n is very important. it is like pressing enter

        try {
            process ?: createSuProcess()
            process ?: return

            with(process!!.outputStream.bufferedWriter()) {
                write("$command\n")
                flush()
            }
        } catch (e: Exception) {
            Timber.e(e)

            e.message?.let { message -> toast(message) }
        }
    }
}