package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.constraints.ConstraintSnapshot
import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.system.phone.CallState
import timber.log.Timber
import java.time.LocalTime

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
    val localTime: LocalTime = LocalTime.now(),
) : ConstraintSnapshot {

    override fun isSatisfied(constraint: Constraint): Boolean {
        val isSatisfied = when (constraint) {
            is Constraint.AppInForeground -> appInForeground == constraint.packageName
            is Constraint.AppNotInForeground -> appInForeground != constraint.packageName
            is Constraint.AppPlayingMedia ->
                appsPlayingMedia.contains(constraint.packageName)

            is Constraint.AppNotPlayingMedia ->
                appsPlayingMedia.none { it == constraint.packageName }

            is Constraint.MediaPlaying -> appsPlayingMedia.isNotEmpty()
            is Constraint.NoMediaPlaying -> appsPlayingMedia.isEmpty()
            is Constraint.BtDeviceConnected -> {
                connectedBluetoothDevices.any { it.address == constraint.bluetoothAddress }
            }

            is Constraint.BtDeviceDisconnected -> {
                connectedBluetoothDevices.none { it.address == constraint.bluetoothAddress }
            }

            is Constraint.OrientationCustom -> orientation == constraint.orientation
            is Constraint.OrientationLandscape ->
                orientation == Orientation.ORIENTATION_90 || orientation == Orientation.ORIENTATION_270

            is Constraint.OrientationPortrait ->
                orientation == Orientation.ORIENTATION_0 || orientation == Orientation.ORIENTATION_180

            is Constraint.ScreenOff -> !isScreenOn
            is Constraint.ScreenOn -> isScreenOn
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

            is Constraint.WifiOff -> !isWifiEnabled
            is Constraint.WifiOn -> isWifiEnabled
            is Constraint.ImeChosen -> chosenImeId == constraint.imeId
            is Constraint.ImeNotChosen -> chosenImeId != constraint.imeId
            is Constraint.DeviceIsLocked -> isLocked
            is Constraint.DeviceIsUnlocked -> !isLocked
            is Constraint.InPhoneCall -> callState == CallState.IN_PHONE_CALL
            is Constraint.NotInPhoneCall -> callState == CallState.NONE
            is Constraint.PhoneRinging -> callState == CallState.RINGING
            is Constraint.Charging -> isCharging
            is Constraint.Discharging -> !isCharging
            is Constraint.LockScreenShowing -> isLockscreenShowing
            is Constraint.LockScreenNotShowing -> !isLockscreenShowing
            is Constraint.Time -> {
                val startTime = constraint.startTime
                val endTime = constraint.endTime

                if (startTime.isAfter(endTime)) {
                    localTime.isAfter(startTime) || localTime.isBefore(endTime)
                } else {
                    localTime.isAfter(startTime) && localTime.isBefore(endTime)
                }
            }
        }

        if (isSatisfied) {
            Timber.d("Constraint satisfied: $constraint")
        } else {
            Timber.d("Constraint not satisfied: $constraint")
        }

        return isSatisfied
    }
}
