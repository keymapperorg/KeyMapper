package io.github.sds100.keymapper.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.WidgetsManager
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.NotificationUtils
import io.github.sds100.keymapper.util.bool
import io.github.sds100.keymapper.util.str
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by sds100 on 24/03/2019.
 */

class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        /* don't show the toggle remappings notification here since it will start when the accessibility service
        starts on boot */
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.apply {
                if (defaultSharedPreferences.getBoolean(
                                str(R.string.key_pref_show_ime_notification),
                                bool(R.bool.default_value_show_ime_notification))) {
                    NotificationUtils.showIMEPickerNotification(this)
                } else {
                    NotificationUtils.dismissNotification(this, NotificationUtils.ID_IME_PERSISTENT)
                }
            }

            WidgetsManager.invalidateNotification(context!!)
        }
    }
}