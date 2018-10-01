package io.github.sds100.keymapper

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.inputmethod.InputMethodManager
import io.github.sds100.keymapper.Utils.RootUtils

/**
 * Created by sds100 on 30/09/2018.
 */

/**
 * Opens the IME picker when a bluetooth device is connected and disconnected
 */
class OpenIMEPickerBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SHOW_IME_PICKER = "action_show_ime_picker"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

            if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED ||
                    intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED ||
                    intent.action == ACTION_SHOW_IME_PICKER) {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    showInputMethodPickerDialog(context!!.applicationContext)
                } else {
                    /* Android Pie doesn't seem to allow you to open the input method picker dialog
                     * from outside the app but it can be achieved by sending a broadcast with a
                     * system process id*/
                    val command = "am broadcast -a com.android.server.InputMethodManagerService.SHOW_INPUT_METHOD_PICKER"
                    RootUtils.executeRootCommand(command)
                }
            }
        }
    }

    private fun showInputMethodPickerDialog(ctx: Context) {
        val imeManager = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        imeManager.showInputMethodPicker()
    }
}