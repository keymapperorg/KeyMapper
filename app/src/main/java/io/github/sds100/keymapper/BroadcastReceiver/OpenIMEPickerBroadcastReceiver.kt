package io.github.sds100.keymapper.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.util.ImeUtils

/**
 * Created by sds100 on 30/09/2018.
 */

class OpenIMEPickerBroadcastReceiver : BroadcastReceiver() {
    companion object {
        /**
         * Only send this action if the app isn't the active window.
         */
        const val ACTION_SHOW_IME_PICKER = "action_show_ime_picker"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        if (intent.action == ACTION_SHOW_IME_PICKER) ImeUtils.showInputMethodPickerDialogOutsideApp(context!!)
    }
}