package io.github.sds100.keymapper.Services

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import io.github.sds100.keymapper.OpenIMEPickerBroadcastReceiver

/**
 * Created by sds100 on 10/07/2018.
 */

/**
 * Listens for when bluetooth devices connect and disconnect
 */
class BluetoothConnectionService : Service() {

    private val mOpenIMEPickerBroadcastReceiver = OpenIMEPickerBroadcastReceiver()

    override fun onCreate() {
        super.onCreate()

        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)

        registerReceiver(mOpenIMEPickerBroadcastReceiver, intentFilter)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        unregisterReceiver(mOpenIMEPickerBroadcastReceiver)
        super.onDestroy()
    }
}