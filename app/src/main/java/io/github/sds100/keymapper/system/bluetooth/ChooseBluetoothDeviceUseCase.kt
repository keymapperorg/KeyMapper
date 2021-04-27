package io.github.sds100.keymapper.system.bluetooth

import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 07/04/2021.
 */

class ChooseBluetoothDeviceUseCaseImpl(adapter: DevicesAdapter):
    ChooseBluetoothDeviceUseCase {
    override val devices: Flow<List<BluetoothDeviceInfo>> = adapter.pairedBluetoothDevices
}

interface ChooseBluetoothDeviceUseCase {
    val devices: Flow<List<BluetoothDeviceInfo>>
}