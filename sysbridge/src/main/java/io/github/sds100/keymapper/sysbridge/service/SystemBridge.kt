package io.github.sds100.keymapper.sysbridge.service

import android.annotation.SuppressLint
import android.ddm.DdmHandleAppName
import android.system.Os
import android.util.Log
import io.github.sds100.keymapper.sysbridge.ISystemBridge
import kotlin.system.exitProcess

@SuppressLint("LogNotTimber")
class SystemBridge : ISystemBridge.Stub() {

    // TODO observe if Key Mapper is uninstalled and stop the process. Look at ApkChangedObservers in Shizuku code.

    external fun stringFromJNI(): String

    companion object {
        private const val TAG: String = "PrivService"

        @JvmStatic
        fun main(args: Array<String>) {
            DdmHandleAppName.setAppName("keymapper_priv", 0)
            SystemBridge()
        }
    }

    init {
        @SuppressLint("UnsafeDynamicallyLoadedCode")
        // TODO can we change "shizuku.library.path" property?
        System.load("${System.getProperty("shizuku.library.path")}/libpriv.so")
        Log.d(TAG, "PrivService started")
    }

    // TODO ungrab all evdev devices
    // TODO ungrab all evdev devices if no key mapper app is bound to the service
    override fun destroy() {
        Log.d(TAG, "PrivService destroyed")
        exitProcess(0)
    }

    override fun sendEvent(): String? {
        Log.d(TAG, "UID = ${Os.getuid()}")
        return stringFromJNI()
    }
}