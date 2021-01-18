package io.github.sds100.keymapper.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.NotificationController
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.util.NotificationUtils

/**
 * Created by sds100 on 24/03/2019.
 */

class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        /* don't show the toggle remappings notification here since it will start when the accessibility service
        starts on boot */
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.apply {
                if (AppPreferences.showImePickerNotification) {
                    NotificationUtils.showIMEPickerNotification(this)
                } else {
                    NotificationUtils.dismissNotification(NotificationUtils.ID_IME_PICKER)
                }
            }

            NotificationController.invalidateNotifications(context!!)
        }
    }
}