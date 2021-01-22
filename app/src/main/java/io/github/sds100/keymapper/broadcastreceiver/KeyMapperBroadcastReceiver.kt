package io.github.sds100.keymapper.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.ui.activity.HomeActivity
import io.github.sds100.keymapper.util.AccessibilityUtils
import io.github.sds100.keymapper.util.KeyboardUtils
import io.github.sds100.keymapper.util.NotificationUtils

/**
 * Created by sds100 on 24/03/2019.
 */

class KeyMapperBroadcastReceiver : BroadcastReceiver() {
    companion object {
        /**
         * Only send this action if the app isn't the active window.
         */
        const val ACTION_SHOW_IME_PICKER = "$PACKAGE_NAME.SHOW_IME_PICKER"
        const val ACTION_TOGGLE_KEYBOARD = "$PACKAGE_NAME.TOGGLE_KEYBOARD"
        const val ACTION_DISMISS_PAUSE_KEYMAPS_NOTIFICATION = "$PACKAGE_NAME.DISMISS_PAUSE_KEYMAPS_NOTIFICATION"
        const val ACTION_ON_FINGERPRINT_FEAT_NOTIFICATION_CLICK = "$PACKAGE_NAME.ACTION_ON_FINGERPRINT_FEAT_NOTIFICATION_CLICK"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_SHOW_IME_PICKER -> KeyboardUtils.showInputMethodPickerDialogOutsideApp()
            ACTION_TOGGLE_KEYBOARD -> KeyboardUtils.toggleCompatibleIme(context)

            MyAccessibilityService.ACTION_START -> AccessibilityUtils.enableService(context)

            MyAccessibilityService.ACTION_STOP -> AccessibilityUtils.disableService(context)

            ACTION_DISMISS_PAUSE_KEYMAPS_NOTIFICATION ->
                NotificationUtils.dismissNotification(NotificationUtils.ID_TOGGLE_KEYMAPS)

            ACTION_ON_FINGERPRINT_FEAT_NOTIFICATION_CLICK -> {
                AppPreferences.approvedFingerprintFeaturePrompt = true

                Intent(context, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(this)
                }
            }

            else -> context.sendBroadcast(Intent(intent?.action))
        }
    }
}