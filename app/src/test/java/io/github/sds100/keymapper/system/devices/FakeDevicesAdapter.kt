package io.github.sds100.keymapper.system.devices

import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.Exception

/**
 * Created by sds100 on 26/04/2021.
 */
class FakeDevicesAdapter : DevicesAdapter {
    override val onInputDeviceConnect = MutableSharedFlow<InputDeviceInfo>()
    override val onInputDeviceDisconnect = MutableSharedFlow<InputDeviceInfo>()

    override val connectedInputDevices = MutableStateFlow<State<List<InputDeviceInfo>>>(State.Loading)

    override val pairedBluetoothDevices: StateFlow<List<BluetoothDeviceInfo>>
        get() = throw Exception()

    override val connectedBluetoothDevices: StateFlow<Set<BluetoothDeviceInfo>>
        get() =throw Exception()

    override fun deviceHasKey(id: Int, keyCode: Int): Boolean {
        throw Exception()    }

    override fun getInputDeviceName(descriptor: String): Result<String> {
        throw Exception()    }
}