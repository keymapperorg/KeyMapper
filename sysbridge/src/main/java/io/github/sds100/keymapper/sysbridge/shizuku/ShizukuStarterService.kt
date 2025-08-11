package io.github.sds100.keymapper.sysbridge.shizuku

import android.annotation.SuppressLint
import android.util.Log
import io.github.sds100.keymapper.sysbridge.IShizukuStarterService
import io.github.sds100.keymapper.sysbridge.starter.SystemBridgeStarter
import kotlin.system.exitProcess

@SuppressLint("LogNotTimber")
class ShizukuStarterService : IShizukuStarterService.Stub() {
    companion object {
        private val TAG = "ShizukuStarterService"
    }

    override fun destroy() {
        Log.i(TAG, "ShizukuStarterService destroyed")

        // Must be last line in this method because it halts the JVM.
        exitProcess(0)
    }

    override fun startSystemBridge(
        scriptPath: String?,
        apkPath: String?,
        libPath: String?,
        packageName: String?
    ) {
        if (scriptPath == null || apkPath == null || libPath == null || packageName == null) {
            return
        }

        try {
            val command =
                SystemBridgeStarter.buildStartCommand(scriptPath, apkPath, libPath, packageName)
            Runtime.getRuntime().exec(command).waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start system bridge", e)
        } finally {
            destroy()
        }
    }
}