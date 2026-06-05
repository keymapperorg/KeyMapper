package io.github.sds100.keymapper.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import timber.log.Timber

class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return

        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Timber.i(
                    "Boot completed broadcast: time since boot = ${SystemClock.elapsedRealtime() / 1000}",
                )
                (context.applicationContext as? BaseKeyMapperApp)?.onBootUnlocked()
            }

            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Timber.i(
                    "Locked boot completed broadcast: time since boot = ${SystemClock.elapsedRealtime() / 1000}",
                )
            }
        }
    }
}
