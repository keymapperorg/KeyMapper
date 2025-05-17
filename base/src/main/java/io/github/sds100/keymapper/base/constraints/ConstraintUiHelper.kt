package io.github.sds100.keymapper.base.constraints

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.common.result.handle
import io.github.sds100.keymapper.common.result.valueIfFailure
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.base.util.TimeUtils
import io.github.sds100.keymapper.base.util.ui.ResourceProvider
import io.github.sds100.keymapper.base.util.ui.compose.ComposeIconInfo
import java.time.format.FormatStyle

class ConstraintUiHelper(
    displayConstraintUseCase: DisplayConstraintUseCase,
    resourceProvider: ResourceProvider,
) : DisplayConstraintUseCase by displayConstraintUseCase,
    ResourceProvider by resourceProvider {

    private val timeFormatter by lazy { TimeUtils.localeDateFormatter(FormatStyle.SHORT) }

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

        is Constraint.MediaPlaying -> getString(R.string.constraint_choose_media_playing)
        is Constraint.NoMediaPlaying -> getString(R.string.constraint_choose_media_not_playing)

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

        is Constraint.OrientationLandscape ->
            getString(R.string.constraint_choose_orientation_landscape)

        is Constraint.OrientationPortrait ->
            getString(R.string.constraint_choose_orientation_portrait)

        is Constraint.ScreenOff ->
            getString(R.string.constraint_screen_off_description)

        is Constraint.ScreenOn ->
            getString(R.string.constraint_screen_on_description)

        is Constraint.FlashlightOff -> if (constraint.lens == CameraLens.FRONT) {
            getString(R.string.constraint_front_flashlight_off_description)
        } else {
            getString(R.string.constraint_flashlight_off_description)
        }

        is Constraint.FlashlightOn -> if (constraint.lens == CameraLens.FRONT) {
            getString(R.string.constraint_front_flashlight_on_description)
        } else {
            getString(R.string.constraint_flashlight_on_description)
        }

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

        is Constraint.WifiOff -> getString(R.string.constraint_wifi_off)
        is Constraint.WifiOn -> getString(R.string.constraint_wifi_on)

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

        is Constraint.DeviceIsLocked -> getString(R.string.constraint_device_is_locked)
        is Constraint.DeviceIsUnlocked -> getString(R.string.constraint_device_is_unlocked)
        is Constraint.InPhoneCall -> getString(R.string.constraint_in_phone_call)
        is Constraint.NotInPhoneCall -> getString(R.string.constraint_not_in_phone_call)
        is Constraint.PhoneRinging -> getString(R.string.constraint_phone_ringing)
        is Constraint.Charging -> getString(R.string.constraint_charging)
        is Constraint.Discharging -> getString(R.string.constraint_discharging)
        is Constraint.LockScreenShowing -> getString(R.string.constraint_lock_screen_showing)
        is Constraint.LockScreenNotShowing -> getString(R.string.constraint_lock_screen_not_showing)
        is Constraint.Time -> getString(
            R.string.constraint_time_formatted,
            arrayOf(
                timeFormatter.format(constraint.startTime),
                timeFormatter.format(constraint.endTime),
            ),
        )
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
