package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.display.DisplayAdapter
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.system.media.MediaAdapter
import io.github.sds100.keymapper.util.firstBlocking

/**
 * Created by sds100 on 17/04/2021.
 */

class DetectConstraintsUseCaseImpl(
    private val accessibilityService: IAccessibilityService,
    private val mediaAdapter: MediaAdapter,
    private val devicesAdapter: DevicesAdapter,
    private val displayAdapter: DisplayAdapter
) : DetectConstraintsUseCase {

    override fun isSatisfied(constraintState: ConstraintState): Boolean {
        when (constraintState.mode) {
            ConstraintMode.AND -> {
                return constraintState.constraints.all { isSatisfied(it) }
            }
            ConstraintMode.OR -> {
                return constraintState.constraints.any { isSatisfied(it) }
            }
        }
    }

    private fun isSatisfied(constraint: Constraint): Boolean {
        return when (constraint) {
            is Constraint.AppInForeground -> accessibilityService.rootNode?.packageName == constraint.packageName
            is Constraint.AppNotInForeground -> accessibilityService.rootNode?.packageName != constraint.packageName
            is Constraint.AppPlayingMedia ->
                mediaAdapter.getPackagesPlayingMedia().contains(constraint.packageName)
            is Constraint.BtDeviceConnected -> {
                devicesAdapter.connectedBluetoothDevices.value.any { it.address == constraint.bluetoothAddress }
            }
            is Constraint.BtDeviceDisconnected -> {
                devicesAdapter.connectedBluetoothDevices.value.none { it.address == constraint.bluetoothAddress }
            }
            is Constraint.OrientationCustom -> displayAdapter.orientation == constraint.orientation
            Constraint.OrientationLandscape -> {
                val orientation = displayAdapter.orientation

                orientation == Orientation.ORIENTATION_90 || orientation == Orientation.ORIENTATION_270
            }
            Constraint.OrientationPortrait -> {
                val orientation = displayAdapter.orientation

                orientation == Orientation.ORIENTATION_0 || orientation == Orientation.ORIENTATION_180
            }
            Constraint.ScreenOff -> !displayAdapter.isScreenOn.firstBlocking()
            Constraint.ScreenOn -> displayAdapter.isScreenOn.firstBlocking()
        }
    }
}

interface DetectConstraintsUseCase {
    fun isSatisfied(constraintState: ConstraintState): Boolean
}