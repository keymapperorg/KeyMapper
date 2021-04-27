package io.github.sds100.keymapper.system.devices

import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by sds100 on 26/04/2021.
 */
class FakeDevicesAdapter : DevicesAdapter {
    override val onInputDeviceConnect = MutableSharedFlow<InputDeviceInfo>()
    override val onInputDeviceDisconnect = MutableSharedFlow<InputDeviceInfo>()

    override val connectedInputDevices = MutableStateFlow<State<List<InputDeviceInfo>>>(State.Loading)

    override val pairedBluetoothDevices: StateFlow<List<BluetoothDeviceInfo>>
        get() = TODO("Not yet implemented")

    override val connectedBluetoothDevices: StateFlow<Set<BluetoothDeviceInfo>>
        get() = TODO("Not yet implemented")

    override fun deviceHasKey(id: Int, keyCode: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun getInputDeviceName(descriptor: String): Result<String> {
        TODO("Not yet implemented")
    }
}