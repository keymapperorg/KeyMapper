package io.github.sds100.keymapper.system.bluetooth

import io.github.sds100.keymapper.system.devices.DevicesAdapter
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by sds100 on 07/04/2021.
 */

class ChooseBluetoothDeviceUseCaseImpl(adapter: DevicesAdapter):
    ChooseBluetoothDeviceUseCase {
    override val devices: StateFlow<List<BluetoothDeviceInfo>> = adapter.pairedBluetoothDevices
}

interface ChooseBluetoothDeviceUseCase {
    val devices: StateFlow<List<BluetoothDeviceInfo>>
}