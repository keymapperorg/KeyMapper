package io.github.sds100.keymapper.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.system.notifications.NotificationController
import javax.inject.Inject

/**
 * Created by sds100 on 24/03/2019.
 */

@AndroidEntryPoint
class BootBroadcastReceiver : BroadcastReceiver() {
    
    /*
    Initializing the controller will update any notifications since it will collect the values
    in the constructor
     */
    @Inject
    lateinit var notificationController: NotificationController
    
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
    }
}