package io.github.sds100.keymapper.priv.service

import android.annotation.SuppressLint
import android.ddm.DdmHandleAppName
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
        System.load("${System.getProperty("shizuku.library.path")}/libevdev.so")
        stringFromJNI()
    }

    override fun destroy() {
        Timber.d("Destroy PrivService")
        exitProcess(0)
    }

    override fun sendEvent(): String? {
        TODO("Not yet implemented")
    }
}