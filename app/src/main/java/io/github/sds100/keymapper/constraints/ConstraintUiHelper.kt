package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.DisplayConstraintUseCase
import io.github.sds100.keymapper.system.camera.CameraLensUtils
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.util.handle
import io.github.sds100.keymapper.util.ui.IconInfo
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.TintType
import io.github.sds100.keymapper.util.valueIfFailure

/**
 * Created by sds100 on 18/03/2021.
 */

class ConstraintUiHelper(
    displayConstraintUseCase: DisplayConstraintUseCase,
    resourceProvider: ResourceProvider
) : DisplayConstraintUseCase by displayConstraintUseCase, ResourceProvider by resourceProvider {

    fun getTitle(constraint: Constraint): String = when (constraint) {
        is Constraint.AppInForeground ->
            getAppName(constraint.packageName).handle(
                onSuccess = { getString(R.string.constraint_app_foreground_description, it) },
                onError = { getString(R.string.constraint_choose_app_foreground) }
            )

        is Constraint.AppNotInForeground ->
            getAppName(constraint.packageName).handle(
                onSuccess = { getString(R.string.constraint_app_not_foreground_description, it) },
                onError = { getString(R.string.constraint_choose_app_not_foreground) }
            )

        is Constraint.AppPlayingMedia ->
            getAppName(constraint.packageName).handle(
                onSuccess = { getString(R.string.constraint_app_playing_media_description, it) },
                onError = { getString(R.string.constraint_choose_app_playing_media) }
            )

        is Constraint.AppNotPlayingMedia ->
            getAppName(constraint.packageName).handle(
                onSuccess = {
                    getString(
                        R.string.constraint_app_not_playing_media_description,
                        it
                    )
                },
                onError = { getString(R.string.constraint_choose_app_playing_media) }
            )

        Constraint.MediaPlaying -> getString(R.string.constraint_choose_media_playing)
        Constraint.NoMediaPlaying -> getString(R.string.constraint_choose_media_not_playing)

        is Constraint.BtDeviceConnected ->
            getString(
                R.string.constraint_bt_device_connected_description,
                constraint.deviceName
            )

        is Constraint.BtDeviceDisconnected ->
            getString(
                R.string.constraint_bt_device_disconnected_description,
                constraint.deviceName
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
            getString(CameraLensUtils.getLabel(constraint.lens))
        )

        is Constraint.FlashlightOn -> getString(
            R.string.constraint_flashlight_on_description,
            getString(CameraLensUtils.getLabel(constraint.lens))
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
    }

    fun getIcon(constraint: Constraint): IconInfo? = when (constraint) {
        is Constraint.AppInForeground -> getAppIconInfo(constraint.packageName)
        is Constraint.AppNotInForeground -> getAppIconInfo(constraint.packageName)
        is Constraint.AppPlayingMedia -> getAppIconInfo(constraint.packageName)
        is Constraint.AppNotPlayingMedia -> getAppIconInfo(constraint.packageName)
        Constraint.MediaPlaying -> IconInfo(
            getDrawable(R.drawable.ic_outline_play_arrow_24),
            TintType.OnSurface
        )

        Constraint.NoMediaPlaying -> IconInfo(
            getDrawable(R.drawable.ic_outline_stop_circle_24),
            TintType.OnSurface
        )

        is Constraint.BtDeviceConnected -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_bluetooth_connected_24),
            tintType = TintType.OnSurface
        )

        is Constraint.BtDeviceDisconnected -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_bluetooth_disabled_24),
            tintType = TintType.OnSurface
        )

        is Constraint.OrientationCustom -> {
            val resId = when (constraint.orientation) {
                Orientation.ORIENTATION_0 -> R.drawable.ic_outline_stay_current_portrait_24
                Orientation.ORIENTATION_90 -> R.drawable.ic_outline_stay_current_landscape_24
                Orientation.ORIENTATION_180 -> R.drawable.ic_outline_stay_current_portrait_24
                Orientation.ORIENTATION_270 -> R.drawable.ic_outline_stay_current_landscape_24
            }

            IconInfo(
                drawable = getDrawable(resId),
                tintType = TintType.OnSurface
            )
        }

        Constraint.OrientationLandscape -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_stay_current_landscape_24),
            tintType = TintType.OnSurface
        )

        Constraint.OrientationPortrait -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_stay_current_portrait_24),
            tintType = TintType.OnSurface
        )

        Constraint.ScreenOff -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_stay_current_portrait_24),
            tintType = TintType.OnSurface
        )

        Constraint.ScreenOn -> IconInfo(
            drawable = getDrawable(R.drawable.ic_baseline_mobile_off_24),
            tintType = TintType.OnSurface
        )

        is Constraint.FlashlightOff -> IconInfo(
            drawable = getDrawable(R.drawable.ic_flashlight_off),
            tintType = TintType.OnSurface
        )

        is Constraint.FlashlightOn -> IconInfo(
            drawable = getDrawable(R.drawable.ic_flashlight),
            tintType = TintType.OnSurface
        )

        is Constraint.WifiConnected -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_wifi_24),
            tintType = TintType.OnSurface
        )
        is Constraint.WifiDisconnected -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_signal_wifi_statusbar_null_24),
            tintType = TintType.OnSurface
        )
        Constraint.WifiOff -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_wifi_off_24),
            tintType = TintType.OnSurface
        )
        Constraint.WifiOn -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_wifi_24),
            tintType = TintType.OnSurface
        )

        is Constraint.ImeChosen -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_keyboard_24),
            tintType = TintType.OnSurface
        )

        is Constraint.ImeNotChosen -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_keyboard_24),
            tintType = TintType.OnSurface
        )

        Constraint.DeviceIsLocked -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_lock_24),
            tintType = TintType.OnSurface
        )

        Constraint.DeviceIsUnlocked -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_lock_open_24),
            tintType = TintType.OnSurface
        )
        Constraint.InPhoneCall -> IconInfo(
            drawable = getDrawable(R.drawable.ic_outline_call_24),
            tintType = TintType.OnSurface
        )
        Constraint.NotInPhoneCall -> IconInfo(
            drawable = getDrawable(R.drawable.ic_baseline_call_end_24),
            tintType = TintType.OnSurface
        )
        Constraint.PhoneRinging -> IconInfo(
            drawable = getDrawable(R.drawable.ic_baseline_ring_volume_24),
            tintType = TintType.OnSurface
        )
        Constraint.Charging -> IconInfo(
            drawable = getDrawable(R.drawable.ic_baseline_battery_charging_full_24),
            tintType = TintType.OnSurface
        )
        Constraint.Discharging -> IconInfo(
            drawable = getDrawable(R.drawable.ic_battery_70),
            tintType = TintType.OnSurface
        )
    }

    private fun getAppIconInfo(packageName: String): IconInfo? {
        return getAppIcon(packageName).handle(
            onSuccess = { IconInfo(it, TintType.None) },
            onError = { null }
        )
    }
}