package io.github.sds100.keymapper.util

/**
 * Created by sds100 on 06/06/20.
 */
interface IConstraintState {
    val currentPackageName: String?
    fun isBluetoothDeviceConnected(address: String): Boolean
    val isScreenOn: Boolean
    val orientation: Int?
    val highestPriorityPackagePlayingMedia: String?
}