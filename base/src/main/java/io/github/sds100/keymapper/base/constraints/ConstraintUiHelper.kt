package io.github.sds100.keymapper.base.constraints

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.TimeUtils
import io.github.sds100.keymapper.common.utils.handle
import io.github.sds100.keymapper.common.utils.valueIfFailure
import io.github.sds100.keymapper.system.camera.CameraLens
import java.time.format.FormatStyle

class ConstraintUiHelper(
    displayConstraintUseCase: DisplayConstraintUseCase,
    resourceProvider: ResourceProvider,
) : DisplayConstraintUseCase by displayConstraintUseCase,
    ResourceProvider by resourceProvider {

    private val timeFormatter by lazy { TimeUtils.localeDateFormatter(FormatStyle.SHORT) }

    fun getTitle(constraint: Constraint): String = when (constraint.data) {
        is ConstraintData.AppInForeground ->
            getAppName(constraint.data.packageName).handle(
                onSuccess = { getString(R.string.constraint_app_foreground_description, it) },
                onError = { getString(R.string.constraint_choose_app_foreground) },
            )

        is ConstraintData.AppNotInForeground ->
            getAppName(constraint.data.packageName).handle(
                onSuccess = { getString(R.string.constraint_app_not_foreground_description, it) },
                onError = { getString(R.string.constraint_choose_app_not_foreground) },
            )

        is ConstraintData.AppPlayingMedia ->
            getAppName(constraint.data.packageName).handle(
                onSuccess = { getString(R.string.constraint_app_playing_media_description, it) },
                onError = { getString(R.string.constraint_choose_app_playing_media) },
            )

        is ConstraintData.AppNotPlayingMedia ->
            getAppName(constraint.data.packageName).handle(
                onSuccess = {
                    getString(
                        R.string.constraint_app_not_playing_media_description,
                        it,
                    )
                },
                onError = { getString(R.string.constraint_choose_app_playing_media) },
            )

        is ConstraintData.MediaPlaying -> getString(R.string.constraint_choose_media_playing)
        is ConstraintData.NoMediaPlaying -> getString(R.string.constraint_choose_media_not_playing)

        is ConstraintData.BtDeviceConnected ->
            getString(
                R.string.constraint_bt_device_connected_description,
                constraint.data.deviceName,
            )

        is ConstraintData.BtDeviceDisconnected ->
            getString(
                R.string.constraint_bt_device_disconnected_description,
                constraint.data.deviceName,
            )

        is ConstraintData.OrientationCustom -> {
            val resId = when (constraint.data.orientation) {
                Orientation.ORIENTATION_0 -> R.string.constraint_choose_orientation_0
                Orientation.ORIENTATION_90 -> R.string.constraint_choose_orientation_90
                Orientation.ORIENTATION_180 -> R.string.constraint_choose_orientation_180
                Orientation.ORIENTATION_270 -> R.string.constraint_choose_orientation_270
            }

            getString(resId)
        }

        is ConstraintData.OrientationLandscape ->
            getString(R.string.constraint_choose_orientation_landscape)

        is ConstraintData.OrientationPortrait ->
            getString(R.string.constraint_choose_orientation_portrait)

        is ConstraintData.ScreenOff ->
            getString(R.string.constraint_screen_off_description)

        is ConstraintData.ScreenOn ->
            getString(R.string.constraint_screen_on_description)

        is ConstraintData.FlashlightOff -> if (constraint.data.lens == CameraLens.FRONT) {
            getString(R.string.constraint_front_flashlight_off_description)
        } else {
            getString(R.string.constraint_flashlight_off_description)
        }

        is ConstraintData.FlashlightOn -> if (constraint.data.lens == CameraLens.FRONT) {
            getString(R.string.constraint_front_flashlight_on_description)
        } else {
            getString(R.string.constraint_flashlight_on_description)
        }

        is ConstraintData.WifiConnected -> {
            if (constraint.data.ssid == null) {
                getString(R.string.constraint_wifi_connected_any_description)
            } else {
                getString(R.string.constraint_wifi_connected_description, constraint.data.ssid)
            }
        }

        is ConstraintData.WifiDisconnected -> {
            if (constraint.data.ssid == null) {
                getString(R.string.constraint_wifi_disconnected_any_description)
            } else {
                getString(R.string.constraint_wifi_disconnected_description, constraint.data.ssid)
            }
        }

        is ConstraintData.WifiOff -> getString(R.string.constraint_wifi_off)
        is ConstraintData.WifiOn -> getString(R.string.constraint_wifi_on)

        is ConstraintData.ImeChosen -> {
            val label = getInputMethodLabel(constraint.data.imeId).valueIfFailure {
                constraint.data.imeLabel
            }

            getString(R.string.constraint_ime_chosen_description, label)
        }

        is ConstraintData.ImeNotChosen -> {
            val label = getInputMethodLabel(constraint.data.imeId).valueIfFailure {
                constraint.data.imeLabel
            }

            getString(R.string.constraint_ime_not_chosen_description, label)
        }

        is ConstraintData.DeviceIsLocked -> getString(R.string.constraint_device_is_locked)
        is ConstraintData.DeviceIsUnlocked -> getString(R.string.constraint_device_is_unlocked)
        is ConstraintData.InPhoneCall -> getString(R.string.constraint_in_phone_call)
        is ConstraintData.NotInPhoneCall -> getString(R.string.constraint_not_in_phone_call)
        is ConstraintData.PhoneRinging -> getString(R.string.constraint_phone_ringing)
        is ConstraintData.Charging -> getString(R.string.constraint_charging)
        is ConstraintData.Discharging -> getString(R.string.constraint_discharging)
        is ConstraintData.LockScreenShowing -> getString(R.string.constraint_lock_screen_showing)
        is ConstraintData.LockScreenNotShowing -> getString(R.string.constraint_lock_screen_not_showing)
        is ConstraintData.Time -> getString(
            R.string.constraint_time_formatted,
            arrayOf(
                timeFormatter.format(constraint.data.startTime),
                timeFormatter.format(constraint.data.endTime),
            ),
        )
    }

    fun getIcon(constraint: Constraint): ComposeIconInfo = when (constraint.data) {
        is ConstraintData.AppInForeground -> getAppIconInfo(constraint.data.packageName)
            ?: ComposeIconInfo.Vector(Icons.Rounded.Android)

        is ConstraintData.AppNotInForeground -> getAppIconInfo(constraint.data.packageName)
            ?: ComposeIconInfo.Vector(Icons.Rounded.Android)

        is ConstraintData.AppPlayingMedia -> getAppIconInfo(constraint.data.packageName)
            ?: ComposeIconInfo.Vector(Icons.Rounded.Android)

        is ConstraintData.AppNotPlayingMedia -> getAppIconInfo(constraint.data.packageName)
            ?: ComposeIconInfo.Vector(Icons.Rounded.Android)

        else -> ConstraintUtils.getIcon(constraint.id)
    }

    private fun getAppIconInfo(packageName: String): ComposeIconInfo? = getAppIcon(packageName).handle(
        onSuccess = { ComposeIconInfo.Drawable(it) },
        onError = { null },
    )
}
