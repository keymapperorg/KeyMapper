package io.github.sds100.keymapper.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.util.OnBootEvent

/**
 * Created by sds100 on 24/03/2019.
 */

class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        /* don't show the toggle remappings notification here since it will start when the accessibility service
        starts on boot */
        context ?: return
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            ServiceLocator.notificationController(context).onEvent(OnBootEvent)
        }
    }
}