package io.github.sds100.keymapper.base.system.bluetooth

import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ChooseBluetoothDeviceUseCaseImpl
    @Inject
    constructor(
        private val devicesAdapter: DevicesAdapter,
        private val permissionAdapter: PermissionAdapter,
    ) : ChooseBluetoothDeviceUseCase {
        override val devices: StateFlow<List<BluetoothDeviceInfo>> =
            devicesAdapter.pairedBluetoothDevices

        override val hasPermissionToSeeDevices: Flow<Boolean> =
            permissionAdapter.isGrantedFlow(Permission.FIND_NEARBY_DEVICES)

        override fun requestPermission() {
            permissionAdapter.request(Permission.FIND_NEARBY_DEVICES)
        }
    }

interface ChooseBluetoothDeviceUseCase {
    val devices: StateFlow<List<BluetoothDeviceInfo>>

    val hasPermissionToSeeDevices: Flow<Boolean>

    fun requestPermission()
}
