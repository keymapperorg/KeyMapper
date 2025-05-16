package io.github.sds100.keymapper.system.bluetooth

import io.github.sds100.keymapper.common.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 14/02/2021.
 */
interface BluetoothAdapter {
    val onDeviceConnect: Flow<BluetoothDeviceInfo>
    val onDeviceDisconnect: Flow<BluetoothDeviceInfo>
    val onDevicePairedChange: Flow<BluetoothDeviceInfo>

    val isBluetoothEnabled: Flow<Boolean>

    fun enable(): Result<*>
    fun disable(): Result<*>
}
