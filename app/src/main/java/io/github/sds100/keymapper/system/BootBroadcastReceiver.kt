package io.github.sds100.keymapper.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.KeyMapperApp

class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return

        if (intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            (context.applicationContext as? KeyMapperApp)?.onBootUnlocked()
        }
    }
}
