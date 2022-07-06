package io.github.sds100.keymapper.system.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Created by sds100 on 28/12/2018.
 */

@AndroidEntryPoint
class BluetoothBroadcastReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var bluetoothAdapter: AndroidBluetoothAdapter

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return

        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED ||
            intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED
        ) {
            bluetoothAdapter.onReceiveIntent(intent)
        }
    }
}