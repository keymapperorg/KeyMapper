package io.github.sds100.keymapper.constraints

import android.media.AudioManager
import android.os.Build
import io.github.sds100.keymapper.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.display.DisplayAdapter
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.lock.LockScreenAdapter
import io.github.sds100.keymapper.system.media.MediaAdapter
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.phone.CallState
import io.github.sds100.keymapper.system.phone.PhoneAdapter
import io.github.sds100.keymapper.system.power.PowerAdapter
import io.github.sds100.keymapper.util.firstBlocking
import timber.log.Timber
import java.time.LocalTime

/**
 * This allows constraints to be checked lazily because some system calls take a significant amount of time.
 */
class LazyConstraintSnapshot(
    accessibilityService: IAccessibilityService,
    mediaAdapter: MediaAdapter,
    devicesAdapter: DevicesAdapter,
    displayAdapter: DisplayAdapter,
    networkAdapter: NetworkAdapter,
    private val cameraAdapter: CameraAdapter,
    inputMethodAdapter: InputMethodAdapter,
    lockScreenAdapter: LockScreenAdapter,
    phoneAdapter: PhoneAdapter,
    powerAdapter: PowerAdapter,
) : ConstraintSnapshot {
    private val appInForeground: String? by lazy { accessibilityService.rootNode?.packageName }
    private val connectedBluetoothDevices: Set<BluetoothDeviceInfo> by lazy { devicesAdapter.connectedBluetoothDevices.value }
    private val orientation: Orientation by lazy { displayAdapter.cachedOrientation }
    private val isScreenOn: Boolean by lazy { displayAdapter.isScreenOn.firstBlocking() }
    private val appsPlayingMedia: List<String> by lazy { mediaAdapter.getActiveMediaSessionPackages() }

    private val audioVolumeStreams: Set<Int> by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mediaAdapter.getActiveAudioVolumeStreams()
        } else {
            emptySet()
        }
    }

    private val isWifiEnabled: Boolean by lazy { networkAdapter.isWifiEnabled() }
    private val connectedWifiSSID: String? by lazy { networkAdapter.connectedWifiSSID }
    private val chosenImeId: String? by lazy { inputMethodAdapter.chosenIme.value?.id }
    private val callState: CallState by lazy { phoneAdapter.getCallState() }
    private val isCharging: Boolean by lazy { powerAdapter.isCharging.value }

    private val isLocked: Boolean by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            lockScreenAdapter.isLocked()
        } else {
            false
        }
    }

    private val isLockscreenShowing: Boolean by lazy {
        lockScreenAdapter.isLockScreenShowing()
    }

    private val localTime = LocalTime.now()

    private fun isMediaPlaying(): Boolean {
        return audioVolumeStreams.contains(AudioManager.STREAM_MUSIC) || appsPlayingMedia.isNotEmpty()
    }

    override fun isSatisfied(constraint: Constraint): Boolean {
        val isSatisfied = when (constraint) {
            is Constraint.AppInForeground -> appInForeground == constraint.packageName
            is Constraint.AppNotInForeground -> appInForeground != constraint.packageName
            is Constraint.AppPlayingMedia -> {
                if (appsPlayingMedia.contains(constraint.packageName)) {
                    return true
                } else if (appInForeground == constraint.packageName && isMediaPlaying()) {
                    return true
                } else {
                    return false
                }
            }

            is Constraint.AppNotPlayingMedia ->
                appsPlayingMedia.none { it == constraint.packageName } &&
                    !(appInForeground == constraint.packageName && isMediaPlaying())

            is Constraint.MediaPlaying -> isMediaPlaying()

            is Constraint.NoMediaPlaying -> !isMediaPlaying()

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
            is Constraint.FlashlightOff -> !cameraAdapter.isFlashlightOn(constraint.lens)
            is Constraint.FlashlightOn -> cameraAdapter.isFlashlightOn(constraint.lens)
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
            is Constraint.InPhoneCall ->
                callState == CallState.IN_PHONE_CALL ||
                    audioVolumeStreams.contains(AudioManager.STREAM_VOICE_CALL)

            is Constraint.NotInPhoneCall ->
                callState == CallState.NONE &&
                    !audioVolumeStreams.contains(AudioManager.STREAM_VOICE_CALL)

            is Constraint.PhoneRinging ->
                callState == CallState.RINGING ||
                    audioVolumeStreams.contains(AudioManager.STREAM_RING)

            is Constraint.Charging -> isCharging
            is Constraint.Discharging -> !isCharging

            // The keyguard manager still reports the lock screen as showing if you are in
            // an another activity like the camera app while the phone is locked.
            is Constraint.LockScreenShowing -> isLockscreenShowing && appInForeground == "com.android.systemui"
            is Constraint.LockScreenNotShowing -> !isLockscreenShowing || appInForeground != "com.android.systemui"

            is Constraint.Time ->
                if (constraint.startTime.isAfter(constraint.endTime)) {
                    localTime.isAfter(constraint.startTime) || localTime.isBefore(constraint.endTime)
                } else {
                    localTime.isAfter(constraint.startTime) && localTime.isBefore(constraint.endTime)
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

interface ConstraintSnapshot {
    fun isSatisfied(constraint: Constraint): Boolean
}

/**
 * Whether multiple constraint states are satisfied. This does an AND on the
 * constraint states.
 */
fun ConstraintSnapshot.isSatisfied(vararg constraintState: ConstraintState): Boolean {
    for (state in constraintState) {
        when (state.mode) {
            ConstraintMode.AND -> {
                for (constraint in state.constraints) {
                    if (!isSatisfied(constraint)) {
                        return false
                    }
                }
            }

            ConstraintMode.OR -> {
                // If no constraints then still satisfied
                if (state.constraints.isEmpty()) {
                    continue
                }

                var anySatisfied = false

                for (constraint in state.constraints) {
                    if (isSatisfied(constraint)) {
                        anySatisfied = true
                        break
                    }
                }

                if (!anySatisfied) {
                    return false
                }
            }
        }
    }

    return true
}
