package io.github.sds100.keymapper.BroadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Utils.NotificationUtils
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by sds100 on 30/09/2018.
 */
class BootBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent!!.action == Intent.ACTION_BOOT_COMPLETED) {
            context!!.apply {
                defaultSharedPreferences.apply {
                    val showNotification =
                            getBoolean(getString(R.string.key_pref_show_notification), true)

                    val showNotificationOnBoot =
                            getBoolean(getString(R.string.key_pref_show_notification_on_boot), true)

                    if (showNotification && showNotificationOnBoot) {
                        NotificationUtils.showIMEPickerNotification(context)
                    }
                }
            }
        }
    }
}