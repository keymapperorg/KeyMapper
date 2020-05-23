package io.github.sds100.keymapper.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice

/**
 * Created by sds100 on 02/10/2018.
 */

object BluetoothUtils {
    /**
     * @return a list of all paired bluetooth devices. It is null if bluetooth is disabled and/or
     * there are not paired devices
     */
    fun getPairedDevices(): List<BluetoothDevice>? {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return null

        return bluetoothAdapter.bondedDevices.toList()
    }

    fun changeBluetoothState(state: StateChange) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        when (state) {
            StateChange.ENABLE -> bluetoothAdapter.enable()
            StateChange.DISABLE -> bluetoothAdapter.disable()
            StateChange.TOGGLE -> {
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