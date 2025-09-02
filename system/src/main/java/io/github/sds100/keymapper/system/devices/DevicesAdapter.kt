package io.github.sds100.keymapper.system.devices

import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DevicesAdapter {
    val onInputDeviceConnect: Flow<InputDeviceInfo>
    val onInputDeviceDisconnect: Flow<InputDeviceInfo>
    val connectedInputDevices: StateFlow<State<List<InputDeviceInfo>>>

    val pairedBluetoothDevices: StateFlow<List<BluetoothDeviceInfo>>
    val connectedBluetoothDevices: StateFlow<Set<BluetoothDeviceInfo>>

    fun deviceHasKey(id: Int, keyCode: Int): Boolean
    fun getInputDeviceName(descriptor: String): KMResult<String>
}
