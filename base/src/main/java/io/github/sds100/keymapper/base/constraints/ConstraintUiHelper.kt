package io.github.sds100.keymapper.base.constraints

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.PhysicalOrientation
import io.github.sds100.keymapper.common.utils.TimeUtils
import io.github.sds100.keymapper.common.utils.handle
import io.github.sds100.keymapper.common.utils.valueIfFailure
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.volume.RingerMode
import java.time.format.FormatStyle

class ConstraintUiHelper(
    displayConstraintUseCase: DisplayConstraintUseCase,
    resourceProvider: ResourceProvider,
) : DisplayConstraintUseCase by displayConstraintUseCase,
    ResourceProvider by resourceProvider {

    private val timeFormatter by lazy { TimeUtils.localeDateFormatter(FormatStyle.SHORT) }

    fun getTitle(constraint: Constraint): String {
        return if (constraint.isNot) {
            getNotTitle(constraint)
        } else {
            getNormalTitle(constraint)
        }
    }

    private fun getNormalTitle(constraint: Constraint): String = when (constraint.data) {
        is ConstraintData.AppInForeground ->
            getAppName(constraint.data.packageName).handle(
                onSuccess = { getString(R.string.constraint_app_foreground_description, it) },
                onError = { getString(R.string.constraint_choose_app_foreground) },
            )

        is ConstraintData.AppPlayingMedia ->
            getAppName(constraint.data.packageName).handle(
                onSuccess = { getString(R.string.constraint_app_playing_media_description, it) },
                onError = { getString(R.string.constraint_choose_app_playing_media) },
            )

        is ConstraintData.MediaPlaying -> getString(R.string.constraint_choose_media_playing)

        is ConstraintData.BtDeviceConnected ->
            getString(
                R.string.constraint_bt_device_connected_description,
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

        is ConstraintData.PhysicalOrientation -> {
            val resId = when (constraint.data.physicalOrientation) {
                PhysicalOrientation.PORTRAIT ->
                    R.string.constraint_choose_physical_orientation_portrait

                PhysicalOrientation.LANDSCAPE ->
                    R.string.constraint_choose_physical_orientation_landscape

                PhysicalOrientation.PORTRAIT_INVERTED ->
                    R.string.constraint_choose_physical_orientation_portrait_inverted

                PhysicalOrientation.LANDSCAPE_INVERTED ->
                    R.string.constraint_choose_physical_orientation_landscape_inverted
            }

            getString(resId)
        }

        is ConstraintData.ScreenOn ->
            getString(R.string.constraint_screen_on_description)

        is ConstraintData.DisplayResolution -> getString(
            R.string.constraint_display_resolution_description,
            arrayOf(constraint.data.width, constraint.data.height),
        )

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

        is ConstraintData.WifiOn -> getString(R.string.constraint_wifi_on)

        is ConstraintData.ImeChosen -> {
            val label = getInputMethodLabel(constraint.data.imeId).valueIfFailure {
                constraint.data.imeLabel
            }

            getString(R.string.constraint_ime_chosen_description, label)
        }

        is ConstraintData.KeyboardShowing -> getString(
            R.string.constraint_keyboard_showing_description,
        )

        is ConstraintData.DeviceIsLocked -> getString(R.string.constraint_device_is_locked)

        is ConstraintData.InPhoneCall -> getString(R.string.constraint_in_phone_call)

        is ConstraintData.NotInPhoneCall -> getString(R.string.constraint_not_in_phone_call)

        is ConstraintData.PhoneRinging -> getString(R.string.constraint_phone_ringing)

        is ConstraintData.RingerMode -> when (constraint.data.ringerMode) {
            RingerMode.NORMAL -> getString(R.string.constraint_ringer_mode_normal)
            RingerMode.VIBRATE -> getString(R.string.constraint_ringer_mode_vibrate)
            RingerMode.SILENT -> getString(R.string.constraint_ringer_mode_silent)
        }

        is ConstraintData.Charging -> getString(R.string.constraint_charging)

        is ConstraintData.HingeClosed -> getString(R.string.constraint_hinge_closed_description)

        is ConstraintData.HingeOpen -> getString(R.string.constraint_hinge_open_description)

        is ConstraintData.LockScreenShowing -> getString(R.string.constraint_lock_screen_showing)

        is ConstraintData.NotificationPanelShowing ->
            getString(R.string.constraint_notification_panel_showing)

        is ConstraintData.NotificationPosted ->
            getNotificationPostedTitle(constraint.data, isNot = false)

        is ConstraintData.Time -> getString(
            R.string.constraint_time_formatted,
            arrayOf(
                timeFormatter.format(constraint.data.startTime),
                timeFormatter.format(constraint.data.endTime),
            ),
        )
    }

    private fun getNotTitle(constraint: Constraint): String = when (constraint.data) {
        is ConstraintData.AppInForeground ->
            getAppName(constraint.data.packageName).handle(
                onSuccess = { getString(R.string.constraint_app_not_foreground_description, it) },
                onError = { getString(R.string.constraint_choose_app_not_foreground) },
            )

        is ConstraintData.AppPlayingMedia ->
            getAppName(constraint.data.packageName).handle(
                onSuccess = {
                    getString(R.string.constraint_app_not_playing_media_description, it)
                },
                onError = { getString(R.string.constraint_choose_app_not_playing_media) },
            )

        is ConstraintData.MediaPlaying ->
            getString(R.string.constraint_media_playing_not_description)

        is ConstraintData.BtDeviceConnected ->
            getString(
                R.string.constraint_bt_device_disconnected_description,
                constraint.data.deviceName,
            )

        is ConstraintData.ScreenOn -> getString(R.string.constraint_screen_off_description)

        is ConstraintData.FlashlightOn -> if (constraint.data.lens == CameraLens.FRONT) {
            getString(R.string.constraint_front_flashlight_off_description)
        } else {
            getString(R.string.constraint_flashlight_off_description)
        }

        is ConstraintData.WifiConnected -> if (constraint.data.ssid == null) {
            getString(R.string.constraint_wifi_disconnected_any_description)
        } else {
            getString(R.string.constraint_wifi_disconnected_description, constraint.data.ssid)
        }

        is ConstraintData.WifiOn -> getString(R.string.constraint_wifi_off)

        is ConstraintData.ImeChosen -> {
            val label = getInputMethodLabel(constraint.data.imeId).valueIfFailure {
                constraint.data.imeLabel
            }

            getString(R.string.constraint_ime_not_chosen_description, label)
        }

        is ConstraintData.KeyboardShowing ->
            getString(R.string.constraint_keyboard_not_showing_description)

        is ConstraintData.DeviceIsLocked -> getString(R.string.constraint_device_is_unlocked)

        is ConstraintData.InPhoneCall -> getString(R.string.constraint_in_phone_call_not)

        is ConstraintData.NotInPhoneCall -> getString(R.string.constraint_not_in_phone_call_not)

        is ConstraintData.LockScreenShowing ->
            getString(R.string.constraint_lock_screen_not_showing)

        is ConstraintData.NotificationPanelShowing ->
            getString(R.string.constraint_notification_panel_not_showing)

        is ConstraintData.Charging -> getString(R.string.constraint_discharging)

        is ConstraintData.PhoneRinging -> getString(R.string.constraint_phone_not_ringing)

        is ConstraintData.RingerMode -> when (constraint.data.ringerMode) {
            RingerMode.NORMAL -> getString(R.string.constraint_ringer_mode_not_normal)
            RingerMode.VIBRATE -> getString(R.string.constraint_ringer_mode_not_vibrate)
            RingerMode.SILENT -> getString(R.string.constraint_ringer_mode_not_silent)
        }

        is ConstraintData.HingeClosed ->
            getString(R.string.constraint_hinge_not_closed_description)

        is ConstraintData.HingeOpen ->
            getString(R.string.constraint_hinge_not_open_description)

        is ConstraintData.NotificationPosted ->
            getNotificationPostedTitle(constraint.data, isNot = true)

        is ConstraintData.Time -> getString(
            R.string.constraint_time_not_formatted,
            arrayOf(
                timeFormatter.format(constraint.data.startTime),
                timeFormatter.format(constraint.data.endTime),
            ),
        )

        is ConstraintData.OrientationCustom -> {
            val resId = when (constraint.data.orientation) {
                Orientation.ORIENTATION_0 -> R.string.constraint_not_orientation_0
                Orientation.ORIENTATION_90 -> R.string.constraint_not_orientation_90
                Orientation.ORIENTATION_180 -> R.string.constraint_not_orientation_180
                Orientation.ORIENTATION_270 -> R.string.constraint_not_orientation_270
            }

            getString(resId)
        }

        is ConstraintData.OrientationLandscape ->
            getString(R.string.constraint_not_orientation_landscape)

        is ConstraintData.OrientationPortrait ->
            getString(R.string.constraint_not_orientation_portrait)

        is ConstraintData.PhysicalOrientation -> {
            val resId = when (constraint.data.physicalOrientation) {
                PhysicalOrientation.PORTRAIT ->
                    R.string.constraint_not_physical_orientation_portrait

                PhysicalOrientation.LANDSCAPE ->
                    R.string.constraint_not_physical_orientation_landscape

                PhysicalOrientation.PORTRAIT_INVERTED ->
                    R.string.constraint_not_physical_orientation_portrait_inverted

                PhysicalOrientation.LANDSCAPE_INVERTED ->
                    R.string.constraint_not_physical_orientation_landscape_inverted
            }

            getString(resId)
        }

        is ConstraintData.DisplayResolution -> getString(
            R.string.constraint_display_resolution_not_description,
            arrayOf(constraint.data.width, constraint.data.height),
        )
    }

    fun getIcon(constraint: Constraint): ComposeIconInfo = when (constraint.data) {
        is ConstraintData.AppInForeground -> getAppIconInfo(constraint.data.packageName)
            ?: ComposeIconInfo.Vector(Icons.Rounded.Android)

        is ConstraintData.AppPlayingMedia -> getAppIconInfo(constraint.data.packageName)
            ?: ComposeIconInfo.Vector(Icons.Rounded.Android)

        is ConstraintData.NotificationPosted.FromApp ->
            getAppIconInfo(constraint.data.packageName)
                ?: ConstraintUtils.getIcon(constraint.id)

        else -> if (constraint.isNot) {
            ConstraintUtils.getNotIcon(constraint.id)
        } else {
            ConstraintUtils.getIcon(constraint.id)
        }
    }

    private fun getNotificationPostedTitle(
        data: ConstraintData.NotificationPosted,
        isNot: Boolean,
    ): String = when (data) {
        is ConstraintData.NotificationPosted.FromApp -> {
            val appName = getAppName(data.packageName).valueIfFailure { data.packageName }

            getString(
                if (isNot) {
                    R.string.constraint_notification_not_posted_from_app
                } else {
                    R.string.constraint_notification_posted_from_app
                },
                appName,
            )
        }

        is ConstraintData.NotificationPosted.TextMatch -> getString(
            if (isNot) {
                R.string.constraint_notification_not_posted_match
            } else {
                R.string.constraint_notification_posted_match
            },
            arrayOf(
                getString(getNotificationFieldNoun(data)),
                getString(getMatchModeVerb(data.matchMode)),
                data.text,
            ),
        )
    }

    @StringRes
    private fun getNotificationFieldNoun(data: ConstraintData.NotificationPosted.TextMatch): Int =
        when (data) {
            is ConstraintData.NotificationPosted.Title -> R.string.notification_field_title
            is ConstraintData.NotificationPosted.Text -> R.string.notification_field_text
        }

    @StringRes
    private fun getMatchModeVerb(matchMode: TextMatchMode): Int = when (matchMode) {
        TextMatchMode.CONTAINS -> R.string.notification_match_mode_contains_sentence
        TextMatchMode.EXACT -> R.string.notification_match_mode_exact_sentence
    }

    private fun getAppIconInfo(packageName: String): ComposeIconInfo? =
        getAppIcon(packageName).handle(
            onSuccess = { ComposeIconInfo.Drawable(it) },
            onError = { null },
        )
}
