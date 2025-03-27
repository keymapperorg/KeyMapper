package io.github.sds100.keymapper.constraints

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.camera.CameraLensUtils
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.util.handle
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.util.valueIfFailure

/**
 * Created by sds100 on 18/03/2021.
 */

class ConstraintUiHelper(
    displayConstraintUseCase: DisplayConstraintUseCase,
    resourceProvider: ResourceProvider,
) : DisplayConstraintUseCase by displayConstraintUseCase,
    ResourceProvider by resourceProvider {

    fun getTitle(constraint: Constraint): String = when (constraint) {
        is Constraint.AppInForeground ->
            getAppName(constraint.packageName).handle(
                onSuccess = { getString(R.string.constraint_app_foreground_description, it) },
                onError = { getString(R.string.constraint_choose_app_foreground) },
            )

        is Constraint.AppNotInForeground ->
            getAppName(constraint.packageName).handle(
                onSuccess = { getString(R.string.constraint_app_not_foreground_description, it) },
                onError = { getString(R.string.constraint_choose_app_not_foreground) },
            )

        is Constraint.AppPlayingMedia ->
            getAppName(constraint.packageName).handle(
                onSuccess = { getString(R.string.constraint_app_playing_media_description, it) },
                onError = { getString(R.string.constraint_choose_app_playing_media) },
            )

        is Constraint.AppNotPlayingMedia ->
            getAppName(constraint.packageName).handle(
                onSuccess = {
                    getString(
                        R.string.constraint_app_not_playing_media_description,
                        it,
                    )
                },
                onError = { getString(R.string.constraint_choose_app_playing_media) },
            )

        Constraint.MediaPlaying -> getString(R.string.constraint_choose_media_playing)
        Constraint.NoMediaPlaying -> getString(R.string.constraint_choose_media_not_playing)

        is Constraint.BtDeviceConnected ->
            getString(
                R.string.constraint_bt_device_connected_description,
                constraint.deviceName,
            )

        is Constraint.BtDeviceDisconnected ->
            getString(
                R.string.constraint_bt_device_disconnected_description,
                constraint.deviceName,
            )

        is Constraint.OrientationCustom -> {
            val resId = when (constraint.orientation) {
                Orientation.ORIENTATION_0 -> R.string.constraint_choose_orientation_0
                Orientation.ORIENTATION_90 -> R.string.constraint_choose_orientation_90
                Orientation.ORIENTATION_180 -> R.string.constraint_choose_orientation_180
                Orientation.ORIENTATION_270 -> R.string.constraint_choose_orientation_270
            }

            getString(resId)
        }

        Constraint.OrientationLandscape ->
            getString(R.string.constraint_choose_orientation_landscape)

        Constraint.OrientationPortrait ->
            getString(R.string.constraint_choose_orientation_portrait)

        Constraint.ScreenOff ->
            getString(R.string.constraint_screen_off_description)

        Constraint.ScreenOn ->
            getString(R.string.constraint_screen_on_description)

        is Constraint.FlashlightOff -> getString(
            R.string.constraint_flashlight_off_description,
            getString(CameraLensUtils.getLabel(constraint.lens)),
        )

        is Constraint.FlashlightOn -> getString(
            R.string.constraint_flashlight_on_description,
            getString(CameraLensUtils.getLabel(constraint.lens)),
        )

        is Constraint.WifiConnected -> {
            if (constraint.ssid == null) {
                getString(R.string.constraint_wifi_connected_any_description)
            } else {
                getString(R.string.constraint_wifi_connected_description, constraint.ssid)
            }
        }

        is Constraint.WifiDisconnected -> {
            if (constraint.ssid == null) {
                getString(R.string.constraint_wifi_disconnected_any_description)
            } else {
                getString(R.string.constraint_wifi_disconnected_description, constraint.ssid)
            }
        }

        Constraint.WifiOff -> getString(R.string.constraint_wifi_off)
        Constraint.WifiOn -> getString(R.string.constraint_wifi_on)

        is Constraint.ImeChosen -> {
            val label = getInputMethodLabel(constraint.imeId).valueIfFailure {
                constraint.imeLabel
            }

            getString(R.string.constraint_ime_chosen_description, label)
        }

        is Constraint.ImeNotChosen -> {
            val label = getInputMethodLabel(constraint.imeId).valueIfFailure {
                constraint.imeLabel
            }

            getString(R.string.constraint_ime_not_chosen_description, label)
        }

        Constraint.DeviceIsLocked -> getString(R.string.constraint_device_is_locked)
        Constraint.DeviceIsUnlocked -> getString(R.string.constraint_device_is_unlocked)
        Constraint.InPhoneCall -> getString(R.string.constraint_in_phone_call)
        Constraint.NotInPhoneCall -> getString(R.string.constraint_not_in_phone_call)
        Constraint.PhoneRinging -> getString(R.string.constraint_phone_ringing)
        Constraint.Charging -> getString(R.string.constraint_charging)
        Constraint.Discharging -> getString(R.string.constraint_discharging)
        Constraint.LockScreenShowing -> getString(R.string.constraint_lock_screen_showing)
        Constraint.LockScreenNotShowing -> getString(R.string.constraint_lock_screen_not_showing)
    }

    fun getIcon(constraint: Constraint): ComposeIconInfo = when (constraint) {
        is Constraint.AppInForeground -> getAppIconInfo(constraint.packageName)
            ?: ComposeIconInfo.Vector(Icons.Rounded.Android)

        is Constraint.AppNotInForeground -> getAppIconInfo(constraint.packageName)
            ?: ComposeIconInfo.Vector(Icons.Rounded.Android)

        is Constraint.AppPlayingMedia -> getAppIconInfo(constraint.packageName)
            ?: ComposeIconInfo.Vector(Icons.Rounded.Android)

        is Constraint.AppNotPlayingMedia -> getAppIconInfo(constraint.packageName)
            ?: ComposeIconInfo.Vector(Icons.Rounded.Android)

        else -> ConstraintUtils.getIcon(constraint.id)
    }

    private fun getAppIconInfo(packageName: String): ComposeIconInfo? = getAppIcon(packageName).handle(
        onSuccess = { ComposeIconInfo.Drawable(it) },
        onError = { null },
    )
}
