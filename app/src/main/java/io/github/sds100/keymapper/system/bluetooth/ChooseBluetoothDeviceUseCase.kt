package io.github.sds100.keymapper.system.bluetooth

import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by sds100 on 07/04/2021.
 */

class ChooseBluetoothDeviceUseCaseImpl(
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
