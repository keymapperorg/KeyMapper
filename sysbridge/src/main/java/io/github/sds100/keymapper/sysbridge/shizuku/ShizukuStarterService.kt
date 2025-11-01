package io.github.sds100.keymapper.sysbridge.shizuku

import android.annotation.SuppressLint
import android.util.Log
import io.github.sds100.keymapper.sysbridge.IShizukuStarterService
import kotlin.system.exitProcess

@SuppressLint("LogNotTimber")
class ShizukuStarterService : IShizukuStarterService.Stub() {
    companion object {
        private val TAG = "ShizukuStarterService"
    }

    init {
        Log.i(TAG, "ShizukuStarterService created")
    }

    override fun destroy() {
        Log.i(TAG, "ShizukuStarterService destroyed")

        // Must be last line in this method because it halts the JVM.
        exitProcess(0)
    }

    override fun executeCommand(command: String?): String? {
        command ?: return null

        val process = Runtime.getRuntime().exec(command)

        val out = with(process.inputStream.bufferedReader()) {
            readText()
        }

        val err = with(process.errorStream.bufferedReader()) {
            readText()
        }

        process.waitFor()

        return "$out\n$err"
    }
}
