package io.github.sds100.keymapper.sysbridge.service

import android.os.Looper

class SystemBridge : BaseSystemBridge() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            @Suppress("DEPRECATION")
            Looper.prepareMainLooper()
            SystemBridge()
            Looper.loop()
        }
    }
}
