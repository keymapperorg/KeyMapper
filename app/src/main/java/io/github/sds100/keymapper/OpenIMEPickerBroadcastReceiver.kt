package io.github.sds100.keymapper

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputMethodManager

/**
 * Created by sds100 on 30/09/2018.
 */

/**
 * Opens the IME picker when a bluetooth device is connected and disconnected
 */
class OpenIMEPickerBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context?, intent: Intent?) {
        if (intent != null) {
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    showInputMethodPickerDialog(ctx!!)
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    showInputMethodPickerDialog(ctx!!)
                }
            }
        }
    }

    private fun showInputMethodPickerDialog(ctx: Context) {
        val imeManager = ctx.getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager

        imeManager.showInputMethodPicker()
    }
}