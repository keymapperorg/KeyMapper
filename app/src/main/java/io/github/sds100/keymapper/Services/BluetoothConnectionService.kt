package io.github.sds100.keymapper.Services

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.view.inputmethod.InputMethodManager

/**
 * Created by sds100 on 10/07/2018.
 */

/**
 * Listens for when specified bluetooth devices connect and disconnect
 */
class BluetoothDeviceConnectionService : Service() {

    private val mBluetoothConnectionReceiver = object : BroadcastReceiver() {
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

    override fun onCreate() {
        super.onCreate()

        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)

        registerReceiver(mBluetoothConnectionReceiver, intentFilter)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        unregisterReceiver(mBluetoothConnectionReceiver)
        super.onDestroy()
    }
}