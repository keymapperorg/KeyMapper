package io.github.sds100.keymapper.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.ImeUtils

/**
 * Created by sds100 on 24/03/2019.
 */

class KeyMapperBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_START_ACCESSIBILITY_SERVICE = "${Constants.PACKAGE_NAME}.START_ACCESSIBILITY_SERVICE"
        const val ACTION_STOP_ACCESSIBILITY_SERVICE = "${Constants.PACKAGE_NAME}.STOP_ACCESSIBILITY_SERVICE"

        /**
         * Only send this action if the app isn't the active window.
         */
        const val ACTION_SHOW_IME_PICKER = "action_show_ime_picker"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_SHOW_IME_PICKER -> ImeUtils.showInputMethodPickerDialogOutsideApp(context!!)

            ACTION_START_ACCESSIBILITY_SERVICE ->
                MyAccessibilityService.enableServiceInSettings()

            ACTION_STOP_ACCESSIBILITY_SERVICE ->
                MyAccessibilityService.disableServiceInSettings()

            else -> context?.sendBroadcast(Intent(intent?.action))
        }
    }
}