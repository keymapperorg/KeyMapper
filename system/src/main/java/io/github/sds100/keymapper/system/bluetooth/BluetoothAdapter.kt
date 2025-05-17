package io.github.sds100.keymapper.system.bluetooth

import io.github.sds100.keymapper.common.util.result.Result
import kotlinx.coroutines.flow.Flow

interface BluetoothAdapter {
    val onDeviceConnect: Flow<BluetoothDeviceInfo>
    val onDeviceDisconnect: Flow<BluetoothDeviceInfo>
    val onDevicePairedChange: Flow<BluetoothDeviceInfo>

    val isBluetoothEnabled: Flow<Boolean>

    fun enable(): Result<*>
    fun disable(): Result<*>
}
