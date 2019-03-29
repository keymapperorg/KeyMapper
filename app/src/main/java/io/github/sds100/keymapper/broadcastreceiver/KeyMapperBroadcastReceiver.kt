package io.github.sds100.keymapper.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.ImeUtils
import io.github.sds100.keymapper.util.RootUtils

/**
 * Created by sds100 on 24/03/2019.
 */

class KeyMapperBroadcastReceiver : BroadcastReceiver() {
    companion object {
        /**
         * Only send this action if the app isn't the active window.
         */
        const val ACTION_SHOW_IME_PICKER = "action_show_ime_picker"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_SHOW_IME_PICKER -> ImeUtils.showInputMethodPickerDialogOutsideApp(context)

            MyAccessibilityService.ACTION_START -> {
                if (RootUtils.checkAppHasRootPermission(context)) {
                    MyAccessibilityService.enableServiceInSettingsRoot()

                } else {
                    MyAccessibilityService.openAccessibilitySettings(context)
                }
            }

            MyAccessibilityService.ACTION_STOP -> {
                if (RootUtils.checkAppHasRootPermission(context)) {
                    MyAccessibilityService.disableServiceInSettingsRoot()

                } else {
                    MyAccessibilityService.openAccessibilitySettings(context)
                }
            }

            else -> context.sendBroadcast(Intent(intent?.action))
        }
    }
}