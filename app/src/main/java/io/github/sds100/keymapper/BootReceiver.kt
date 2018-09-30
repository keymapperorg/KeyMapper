package io.github.sds100.keymapper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.Utils.NotificationUtils

/**
 * Created by sds100 on 30/09/2018.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent!!.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationUtils.showIMEPickerNotification(context!!)
        }
    }
}