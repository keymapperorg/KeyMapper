package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import io.github.sds100.keymapper.system.display.Orientation

/**
 * Created by sds100 on 08/05/2021.
 */
data class ConstraintSnapshot(
    val appInForeground: String?,
    val connectedBluetoothDevices: Set<BluetoothDeviceInfo>,
    val orientation: Orientation,
    val isScreenOn: Boolean,
    val appsPlayingMedia: List<String>
) {
    fun isSatisfied(constraint: Constraint): Boolean {
        return when (constraint) {
            is Constraint.AppInForeground -> appInForeground == constraint.packageName
            is Constraint.AppNotInForeground -> appInForeground != constraint.packageName
            is Constraint.AppPlayingMedia ->
                appsPlayingMedia.contains(constraint.packageName)
            is Constraint.BtDeviceConnected -> {
                connectedBluetoothDevices.any { it.address == constraint.bluetoothAddress }
            }
            is Constraint.BtDeviceDisconnected -> {
                connectedBluetoothDevices.none { it.address == constraint.bluetoothAddress }
            }
            is Constraint.OrientationCustom -> orientation == constraint.orientation
            Constraint.OrientationLandscape ->
                orientation == Orientation.ORIENTATION_90 || orientation == Orientation.ORIENTATION_270
            Constraint.OrientationPortrait ->
                orientation == Orientation.ORIENTATION_0 || orientation == Orientation.ORIENTATION_180
            Constraint.ScreenOff -> !isScreenOn
            Constraint.ScreenOn -> isScreenOn
        }
    }

    fun isSatisfied(constraintState: ConstraintState): Boolean {
        return when (constraintState.mode) {
            ConstraintMode.AND -> {
                constraintState.constraints.all { isSatisfied(it) }
            }
            ConstraintMode.OR -> {
                constraintState.constraints.any { isSatisfied(it) }
            }
        }
    }
}
