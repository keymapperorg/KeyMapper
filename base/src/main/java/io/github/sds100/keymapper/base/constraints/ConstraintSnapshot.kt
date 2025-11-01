package io.github.sds100.keymapper.base.constraints

import android.media.AudioManager
import android.os.Build
import io.github.sds100.keymapper.base.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.display.DisplayAdapter
import io.github.sds100.keymapper.system.foldable.FoldableAdapter
import io.github.sds100.keymapper.system.foldable.HingeState
import io.github.sds100.keymapper.system.foldable.isClosed
import io.github.sds100.keymapper.system.foldable.isOpen
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.lock.LockScreenAdapter
import io.github.sds100.keymapper.system.media.MediaAdapter
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.phone.CallState
import io.github.sds100.keymapper.system.phone.PhoneAdapter
import io.github.sds100.keymapper.system.power.PowerAdapter
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
    private val foldableAdapter: FoldableAdapter,
) : ConstraintSnapshot {
    private val appInForeground: String? by lazy { accessibilityService.rootNode?.packageName }
    private val connectedBluetoothDevices: Set<BluetoothDeviceInfo> by lazy {
        devicesAdapter.connectedBluetoothDevices.value
    }
    private val orientation: Orientation by lazy { displayAdapter.cachedOrientation }
    private val isScreenOn: Boolean by lazy { displayAdapter.isScreenOn.firstBlocking() }
    private val appsPlayingMedia: List<String> by lazy {
        mediaAdapter.getActiveMediaSessionPackages()
    }

    private val audioVolumeStreams: Set<Int> by lazy {
        mediaAdapter.getActiveAudioVolumeStreams()
    }

    private val isWifiEnabled: Boolean by lazy { networkAdapter.isWifiEnabled() }
    private val connectedWifiSSID: String? by lazy {
        networkAdapter.connectedWifiSSIDFlow.firstBlocking()
    }
    private val chosenImeId: String? by lazy { inputMethodAdapter.chosenIme.value?.id }
    private val callState: CallState by lazy { phoneAdapter.getCallState() }
    private val isCharging: Boolean by lazy { powerAdapter.isCharging.value }

    private val isLocked: Boolean by lazy {
        lockScreenAdapter.isLocked()
    }

    private val isLockscreenShowing: Boolean by lazy {
        lockScreenAdapter.isLockScreenShowing()
    }

    private val localTime = LocalTime.now()

    private fun isMediaPlaying(): Boolean {
        return audioVolumeStreams.contains(AudioManager.STREAM_MUSIC) ||
            appsPlayingMedia.isNotEmpty()
    }

    override fun isSatisfied(constraint: Constraint): Boolean {
        val isSatisfied = when (constraint.data) {
            is ConstraintData.AppInForeground -> appInForeground == constraint.data.packageName
            is ConstraintData.AppNotInForeground -> appInForeground != constraint.data.packageName
            is ConstraintData.AppPlayingMedia -> {
                if (appsPlayingMedia.contains(constraint.data.packageName)) {
                    return true
                } else if (appInForeground == constraint.data.packageName && isMediaPlaying()) {
                    return true
                } else {
                    return false
                }
            }

            is ConstraintData.AppNotPlayingMedia ->
                appsPlayingMedia.none { it == constraint.data.packageName } &&
                    !(appInForeground == constraint.data.packageName && isMediaPlaying())

            is ConstraintData.MediaPlaying -> isMediaPlaying()

            is ConstraintData.NoMediaPlaying -> !isMediaPlaying()

            is ConstraintData.BtDeviceConnected -> {
                connectedBluetoothDevices.any { it.address == constraint.data.bluetoothAddress }
            }

            is ConstraintData.BtDeviceDisconnected -> {
                connectedBluetoothDevices.none { it.address == constraint.data.bluetoothAddress }
            }

            is ConstraintData.OrientationCustom -> orientation == constraint.data.orientation
            is ConstraintData.OrientationLandscape ->
                orientation == Orientation.ORIENTATION_90 ||
                    orientation == Orientation.ORIENTATION_270

            is ConstraintData.OrientationPortrait ->
                orientation == Orientation.ORIENTATION_0 ||
                    orientation == Orientation.ORIENTATION_180

            is ConstraintData.ScreenOff -> !isScreenOn
            is ConstraintData.ScreenOn -> isScreenOn
            is ConstraintData.FlashlightOff -> !cameraAdapter.isFlashlightOn(constraint.data.lens)
            is ConstraintData.FlashlightOn -> cameraAdapter.isFlashlightOn(constraint.data.lens)
            is ConstraintData.WifiConnected -> {
                if (constraint.data.ssid == null) {
                    // connected to any network
                    connectedWifiSSID != null
                } else {
                    connectedWifiSSID == constraint.data.ssid
                }
            }

            is ConstraintData.WifiDisconnected ->
                if (constraint.data.ssid == null) {
                    // connected to no network
                    connectedWifiSSID == null
                } else {
                    connectedWifiSSID != constraint.data.ssid
                }

            is ConstraintData.WifiOff -> !isWifiEnabled
            is ConstraintData.WifiOn -> isWifiEnabled
            is ConstraintData.ImeChosen -> chosenImeId == constraint.data.imeId
            is ConstraintData.ImeNotChosen -> chosenImeId != constraint.data.imeId
            is ConstraintData.DeviceIsLocked -> isLocked
            is ConstraintData.DeviceIsUnlocked -> !isLocked
            is ConstraintData.InPhoneCall ->
                callState == CallState.IN_PHONE_CALL ||
                    audioVolumeStreams.contains(AudioManager.STREAM_VOICE_CALL)

            is ConstraintData.NotInPhoneCall ->
                callState == CallState.NONE &&
                    !audioVolumeStreams.contains(AudioManager.STREAM_VOICE_CALL)

            is ConstraintData.PhoneRinging ->
                callState == CallState.RINGING ||
                    audioVolumeStreams.contains(AudioManager.STREAM_RING)

            is ConstraintData.Charging -> isCharging
            is ConstraintData.Discharging -> !isCharging

            is ConstraintData.HingeClosed -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    when (val state = foldableAdapter.hingeState.value) {
                        is HingeState.Available -> state.isClosed()
                        is HingeState.Unavailable -> false
                    }
                } else {
                    false
                }
            }

            is ConstraintData.HingeOpen -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    when (val state = foldableAdapter.hingeState.value) {
                        is HingeState.Available -> state.isOpen()
                        is HingeState.Unavailable -> false
                    }
                } else {
                    false
                }
            }

            // The keyguard manager still reports the lock screen as showing if you are in
            // an another activity like the camera app while the phone is locked.
            is ConstraintData.LockScreenShowing ->
                isLockscreenShowing &&
                    appInForeground == "com.android.systemui"
            is ConstraintData.LockScreenNotShowing ->
                !isLockscreenShowing ||
                    appInForeground != "com.android.systemui"

            is ConstraintData.Time ->
                if (constraint.data.startTime.isAfter(constraint.data.endTime)) {
                    localTime.isAfter(constraint.data.startTime) ||
                        localTime.isBefore(constraint.data.endTime)
                } else {
                    localTime.isAfter(constraint.data.startTime) &&
                        localTime.isBefore(constraint.data.endTime)
                }
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
