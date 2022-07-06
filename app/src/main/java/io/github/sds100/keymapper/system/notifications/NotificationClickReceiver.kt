package io.github.sds100.keymapper.system.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Created by sds100 on 24/03/2019.
 */

@AndroidEntryPoint
class NotificationClickReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationAdapter: AndroidNotificationAdapter

    override fun onReceive(context: Context, intent: Intent?) {
        intent ?: return

        notificationAdapter.onReceiveNotificationActionIntent(intent)

        //dismiss the notification drawer after tapping on the notification. This is deprecated on S+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS).apply {
                context.sendBroadcast(this)
            }
        }
    }
}