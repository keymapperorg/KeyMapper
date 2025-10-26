package io.github.sds100.keymapper.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        context ?: return

        if (intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            (context.applicationContext as? BaseKeyMapperApp)?.onBootUnlocked()
        }
    }
}
