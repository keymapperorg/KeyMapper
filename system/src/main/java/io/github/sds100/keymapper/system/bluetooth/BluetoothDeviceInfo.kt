package io.github.sds100.keymapper.system.bluetooth

import kotlinx.serialization.Serializable

@Serializable
data class BluetoothDeviceInfo(val address: String, val name: String)
