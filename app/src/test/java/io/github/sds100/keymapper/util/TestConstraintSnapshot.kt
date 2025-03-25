package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.constraints.ConstraintSnapshot
import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.system.phone.CallState
import timber.log.Timber

class TestConstraintSnapshot(
    val appInForeground: String? = null,
    val connectedBluetoothDevices: Set<BluetoothDeviceInfo> = emptySet(),
    val orientation: Orientation = Orientation.ORIENTATION_0,
    val isScreenOn: Boolean = false,
    val appsPlayingMedia: List<String> = emptyList(),
    val isWifiEnabled: Boolean = false,
    val connectedWifiSSID: String? = null,
    val chosenImeId: String? = null,
    val callState: CallState = CallState.NONE,
    val isCharging: Boolean = false,
    val isLocked: Boolean = false,
    val isBackFlashlightOn: Boolean = false,
    val isFrontFlashlightOn: Boolean = false,
    val isLockscreenShowing: Boolean = false,
) : ConstraintSnapshot {

    override fun isSatisfied(constraint: Constraint): Boolean {
        val isSatisfied = when (constraint) {
            is Constraint.AppInForeground -> appInForeground == constraint.packageName
            is Constraint.AppNotInForeground -> appInForeground != constraint.packageName
            is Constraint.AppPlayingMedia ->
                appsPlayingMedia.contains(constraint.packageName)

            is Constraint.AppNotPlayingMedia ->
                appsPlayingMedia.none { it == constraint.packageName }

            Constraint.MediaPlaying -> appsPlayingMedia.isNotEmpty()
            Constraint.NoMediaPlaying -> appsPlayingMedia.isEmpty()
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
            is Constraint.FlashlightOff -> when (constraint.lens) {
                CameraLens.BACK -> !isBackFlashlightOn
                CameraLens.FRONT -> !isFrontFlashlightOn
            }

            is Constraint.FlashlightOn -> when (constraint.lens) {
                CameraLens.BACK -> isBackFlashlightOn
                CameraLens.FRONT -> isFrontFlashlightOn
            }

            is Constraint.WifiConnected -> {
                if (constraint.ssid == null) {
                    // connected to any network
                    connectedWifiSSID != null
                } else {
                    connectedWifiSSID == constraint.ssid
                }
            }

            is Constraint.WifiDisconnected ->
                if (constraint.ssid == null) {
                    // connected to no network
                    connectedWifiSSID == null
                } else {
                    connectedWifiSSID != constraint.ssid
                }

            Constraint.WifiOff -> !isWifiEnabled
            Constraint.WifiOn -> isWifiEnabled
            is Constraint.ImeChosen -> chosenImeId == constraint.imeId
            is Constraint.ImeNotChosen -> chosenImeId != constraint.imeId
            Constraint.DeviceIsLocked -> isLocked
            Constraint.DeviceIsUnlocked -> !isLocked
            Constraint.InPhoneCall -> callState == CallState.IN_PHONE_CALL
            Constraint.NotInPhoneCall -> callState == CallState.NONE
            Constraint.PhoneRinging -> callState == CallState.RINGING
            Constraint.Charging -> isCharging
            Constraint.Discharging -> !isCharging
            Constraint.LockScreenShowing -> isLockscreenShowing
            Constraint.LockScreenNotShowing -> !isLockscreenShowing
        }

        if (isSatisfied) {
            Timber.d("Constraint satisfied: $constraint")
        } else {
            Timber.d("Constraint not satisfied: $constraint")
        }

        return isSatisfied
    }
}
