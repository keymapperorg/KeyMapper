package io.github.sds100.keymapper.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import io.github.sds100.keymapper.StateChange
import io.github.sds100.keymapper.StateChange.*

/**
 * Created by sds100 on 02/10/2018.
 */

object BluetoothUtils {
    /**
     * @return a list of all paired bluetooth devices. It is null if bluetooth is disabled and/or
     * there are not paired devices
     */
    fun getPairedDevices(): List<BluetoothDevice>? {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            return null
        }

        return bluetoothAdapter.bondedDevices.toList()
    }

    fun changeBluetoothState(stateChange: StateChange) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        when (stateChange) {
            ENABLE -> bluetoothAdapter.enable()
            DISABLE -> bluetoothAdapter.disable()
            TOGGLE -> {
                val isEnabled = bluetoothAdapter.isEnabled

                if (isEnabled) {
                    bluetoothAdapter.disable()
                } else {
                    bluetoothAdapter.enable()
                }
            }
        }
    }
}