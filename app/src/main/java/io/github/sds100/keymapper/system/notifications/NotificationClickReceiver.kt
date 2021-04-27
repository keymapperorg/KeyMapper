package io.github.sds100.keymapper.system.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.ServiceLocator

/**
 * Created by sds100 on 24/03/2019.
 */

class NotificationClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        intent ?: return

        ServiceLocator.notificationAdapter(context).onReceiveNotificationActionIntent(intent)
    }
}