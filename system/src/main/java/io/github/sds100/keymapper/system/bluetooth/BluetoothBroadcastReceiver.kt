package io.github.sds100.keymapper.system.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sds100.keymapper.KeyMapperApp

class BluetoothBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return

        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED ||
            intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED
        ) {
            (context.applicationContext as KeyMapperApp).bluetoothMonitor.onReceiveIntent(intent)
        }
    }
}
