package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.display.DisplayAdapter
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.system.media.MediaAdapter
import io.github.sds100.keymapper.util.firstBlocking

/**
 * Created by sds100 on 08/05/2021.
 */

/**
 * This allows constraints to be checked lazily because some system calls take a significant amount of time.
 */
class ConstraintSnapshot(
    accessibilityService: IAccessibilityService,
    mediaAdapter: MediaAdapter,
    devicesAdapter: DevicesAdapter,
    displayAdapter: DisplayAdapter,
    private val cameraAdapter: CameraAdapter
) {
    private val appInForeground: String? by lazy { accessibilityService.rootNode?.packageName }
    private val connectedBluetoothDevices: Set<BluetoothDeviceInfo> by lazy { devicesAdapter.connectedBluetoothDevices.value }
    private val orientation: Orientation by lazy { displayAdapter.orientation }
    private val isScreenOn: Boolean by lazy { displayAdapter.isScreenOn.firstBlocking() }
    private val appsPlayingMedia: List<String> by lazy { mediaAdapter.getPackagesPlayingMedia() }

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
            is Constraint.FlashlightOff -> !cameraAdapter.isFlashlightOn(constraint.lens)
            is Constraint.FlashlightOn -> cameraAdapter.isFlashlightOn(constraint.lens)
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
