package io.github.sds100.keymapper.priv.service

import android.annotation.SuppressLint
import android.ddm.DdmHandleAppName
import android.system.Os
import io.github.sds100.keymapper.priv.IPrivService
import timber.log.Timber
import kotlin.system.exitProcess

class PrivService : IPrivService.Stub() {

    /**
     * A native method that is implemented by the 'nativelib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        private const val TAG: String = "PrivService"

        @JvmStatic
        fun main(args: Array<String>) {
            DdmHandleAppName.setAppName("keymapper_evdev", 0)
            PrivService()
        }
    }

    init {
        @SuppressLint("UnsafeDynamicallyLoadedCode")
        // TODO can we change "shizuku.library.path" property?
        System.load("${System.getProperty("shizuku.library.path")}/libevdev.so")
        stringFromJNI()
    }

    override fun destroy() {
        Timber.d("Destroy PrivService")
        exitProcess(0)
    }

    override fun sendEvent(): String? {
        Timber.e("UID = ${Os.getuid()}")
        return stringFromJNI()
    }
}