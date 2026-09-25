package io.github.sds100.keymapper.base.constraints

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Battery2Bar
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardHide
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MobileOff
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RingVolume
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.SignalWifiStatusbarNull
import androidx.compose.material.icons.outlined.StayCurrentLandscape
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.rounded.Android
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo

object ConstraintUtils {

    @StringRes
    fun getCategoryLabel(category: ConstraintCategory): Int = when (category) {
        ConstraintCategory.APPS -> R.string.constraint_cat_apps
        ConstraintCategory.MEDIA -> R.string.constraint_cat_media
        ConstraintCategory.BLUETOOTH -> R.string.constraint_cat_bluetooth
        ConstraintCategory.DISPLAY -> R.string.constraint_cat_display
        ConstraintCategory.FLASHLIGHT -> R.string.constraint_cat_flashlight
        ConstraintCategory.WIFI -> R.string.constraint_cat_wifi
        ConstraintCategory.KEYBOARD -> R.string.constraint_cat_keyboard
        ConstraintCategory.LOCK -> R.string.constraint_cat_lock
        ConstraintCategory.PHONE -> R.string.constraint_cat_phone
        ConstraintCategory.POWER -> R.string.constraint_cat_power
        ConstraintCategory.DEVICE -> R.string.constraint_cat_device
        ConstraintCategory.NOTIFICATIONS -> R.string.constraint_cat_notifications
        ConstraintCategory.TIME -> R.string.constraint_cat_time
    }

    fun getCategory(constraintId: ConstraintId): ConstraintCategory = when (constraintId) {
        ConstraintId.APP_IN_FOREGROUND,
        ConstraintId.APP_PLAYING_MEDIA,
            -> ConstraintCategory.APPS

        ConstraintId.MEDIA_PLAYING -> ConstraintCategory.MEDIA

        ConstraintId.BT_DEVICE_CONNECTED -> ConstraintCategory.BLUETOOTH

        ConstraintId.SCREEN_ON,
        ConstraintId.DISPLAY_ORIENTATION_PORTRAIT,
        ConstraintId.DISPLAY_ORIENTATION_LANDSCAPE,
        ConstraintId.DISPLAY_ORIENTATION_0,
        ConstraintId.DISPLAY_ORIENTATION_90,
        ConstraintId.DISPLAY_ORIENTATION_180,
        ConstraintId.DISPLAY_ORIENTATION_270,
        ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT,
        ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE,
        ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT_INVERTED,
        ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE_INVERTED,
        ConstraintId.DISPLAY_RESOLUTION,
            -> ConstraintCategory.DISPLAY

        ConstraintId.FLASHLIGHT_ON -> ConstraintCategory.FLASHLIGHT

        ConstraintId.WIFI_ON,
        ConstraintId.WIFI_CONNECTED,
            -> ConstraintCategory.WIFI

        ConstraintId.IME_CHOSEN,
        ConstraintId.KEYBOARD_SHOWING,
            -> ConstraintCategory.KEYBOARD

        ConstraintId.DEVICE_IS_LOCKED,
        ConstraintId.LOCK_SCREEN_SHOWING,
            -> ConstraintCategory.LOCK

        ConstraintId.IN_PHONE_CALL,
        ConstraintId.NOT_IN_PHONE_CALL,
        ConstraintId.PHONE_RINGING,
        ConstraintId.RINGER_MODE_NORMAL,
        ConstraintId.RINGER_MODE_VIBRATE,
        ConstraintId.RINGER_MODE_SILENT,
            -> ConstraintCategory.PHONE

        ConstraintId.CHARGING -> ConstraintCategory.POWER

        ConstraintId.HINGE_CLOSED,
        ConstraintId.HINGE_OPEN,
            -> ConstraintCategory.DEVICE

        ConstraintId.NOTIFICATION_PANEL_SHOWING,
        ConstraintId.NOTIFICATION_POSTED,
            -> ConstraintCategory.NOTIFICATIONS

        ConstraintId.TIME -> ConstraintCategory.TIME
    }

    fun getIcon(constraintId: ConstraintId): ComposeIconInfo = when (constraintId) {
        ConstraintId.APP_IN_FOREGROUND,
        ConstraintId.APP_PLAYING_MEDIA,
            -> ComposeIconInfo.Vector(Icons.Rounded.Android)

        ConstraintId.MEDIA_PLAYING -> ComposeIconInfo.Vector(Icons.Outlined.PlayArrow)

        ConstraintId.BT_DEVICE_CONNECTED -> ComposeIconInfo.Vector(
            Icons.Outlined.BluetoothConnected,
        )

        ConstraintId.DISPLAY_ORIENTATION_0,
        ConstraintId.DISPLAY_ORIENTATION_180,
            -> ComposeIconInfo.Vector(Icons.Outlined.StayCurrentPortrait)

        ConstraintId.DISPLAY_ORIENTATION_90,
        ConstraintId.DISPLAY_ORIENTATION_270,
            -> ComposeIconInfo.Vector(Icons.Outlined.StayCurrentLandscape)

        ConstraintId.DISPLAY_ORIENTATION_LANDSCAPE -> ComposeIconInfo.Vector(
            Icons.Outlined.StayCurrentLandscape,
        )

        ConstraintId.DISPLAY_ORIENTATION_PORTRAIT -> ComposeIconInfo.Vector(
            Icons.Outlined.StayCurrentPortrait,
        )

        ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT,
        ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT_INVERTED,
            -> ComposeIconInfo.Vector(Icons.Outlined.StayCurrentPortrait)

        ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE,
        ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE_INVERTED,
            -> ComposeIconInfo.Vector(Icons.Outlined.StayCurrentLandscape)

        ConstraintId.SCREEN_ON -> ComposeIconInfo.Vector(Icons.Outlined.StayCurrentPortrait)

        ConstraintId.DISPLAY_RESOLUTION -> ComposeIconInfo.Vector(Icons.Outlined.AspectRatio)

        ConstraintId.FLASHLIGHT_ON -> ComposeIconInfo.Vector(Icons.Outlined.FlashlightOn)

        ConstraintId.WIFI_CONNECTED -> ComposeIconInfo.Vector(Icons.Outlined.Wifi)

        ConstraintId.WIFI_ON -> ComposeIconInfo.Vector(Icons.Outlined.Wifi)

        ConstraintId.IME_CHOSEN -> ComposeIconInfo.Vector(Icons.Outlined.Keyboard)

        ConstraintId.KEYBOARD_SHOWING -> ComposeIconInfo.Vector(Icons.Outlined.Keyboard)

        ConstraintId.DEVICE_IS_LOCKED -> ComposeIconInfo.Vector(Icons.Outlined.Lock)

        ConstraintId.IN_PHONE_CALL -> ComposeIconInfo.Vector(Icons.Outlined.Call)

        ConstraintId.NOT_IN_PHONE_CALL -> ComposeIconInfo.Vector(Icons.Outlined.CallEnd)

        ConstraintId.PHONE_RINGING -> ComposeIconInfo.Vector(Icons.Outlined.RingVolume)

        ConstraintId.RINGER_MODE_NORMAL -> ComposeIconInfo.Vector(Icons.Outlined.Notifications)

        ConstraintId.RINGER_MODE_VIBRATE -> ComposeIconInfo.Vector(Icons.Outlined.Vibration)

        ConstraintId.RINGER_MODE_SILENT -> ComposeIconInfo.Vector(Icons.Outlined.NotificationsOff)

        ConstraintId.CHARGING -> ComposeIconInfo.Vector(Icons.Outlined.BatteryChargingFull)

        ConstraintId.HINGE_CLOSED -> ComposeIconInfo.Vector(Icons.Outlined.StayCurrentPortrait)

        ConstraintId.HINGE_OPEN -> ComposeIconInfo.Vector(Icons.Outlined.StayCurrentLandscape)

        ConstraintId.LOCK_SCREEN_SHOWING -> ComposeIconInfo.Vector(
            Icons.Outlined.ScreenLockPortrait,
        )

        ConstraintId.NOTIFICATION_PANEL_SHOWING ->
            ComposeIconInfo.Vector(Icons.Outlined.Notifications)

        ConstraintId.NOTIFICATION_POSTED ->
            ComposeIconInfo.Vector(Icons.Outlined.NotificationsActive)

        ConstraintId.TIME -> ComposeIconInfo.Vector(Icons.Outlined.Timer)
    }

    fun getNotIcon(constraintId: ConstraintId): ComposeIconInfo = when (constraintId) {
        ConstraintId.MEDIA_PLAYING -> ComposeIconInfo.Vector(Icons.Outlined.StopCircle)

        ConstraintId.BT_DEVICE_CONNECTED -> ComposeIconInfo.Vector(
            Icons.Outlined.BluetoothDisabled,
        )

        ConstraintId.SCREEN_ON -> ComposeIconInfo.Vector(Icons.Outlined.MobileOff)

        ConstraintId.FLASHLIGHT_ON -> ComposeIconInfo.Vector(Icons.Outlined.FlashlightOff)

        ConstraintId.WIFI_CONNECTED -> ComposeIconInfo.Vector(
            Icons.Outlined.SignalWifiStatusbarNull,
        )

        ConstraintId.WIFI_ON -> ComposeIconInfo.Vector(Icons.Outlined.WifiOff)

        ConstraintId.KEYBOARD_SHOWING -> ComposeIconInfo.Vector(Icons.Outlined.KeyboardHide)

        ConstraintId.DEVICE_IS_LOCKED -> ComposeIconInfo.Vector(Icons.Outlined.LockOpen)

        ConstraintId.LOCK_SCREEN_SHOWING -> ComposeIconInfo.Vector(Icons.Outlined.LockOpen)

        ConstraintId.CHARGING -> ComposeIconInfo.Vector(Icons.Outlined.Battery2Bar)

        ConstraintId.NOTIFICATION_PANEL_SHOWING -> ComposeIconInfo.Vector(
            Icons.Outlined.NotificationsOff,
        )

        ConstraintId.IN_PHONE_CALL -> ComposeIconInfo.Vector(Icons.Outlined.CallEnd)

        ConstraintId.NOT_IN_PHONE_CALL -> ComposeIconInfo.Vector(Icons.Outlined.Call)

        ConstraintId.PHONE_RINGING -> ComposeIconInfo.Vector(Icons.Outlined.CallEnd)

        ConstraintId.NOTIFICATION_POSTED ->
            ComposeIconInfo.Vector(Icons.Outlined.NotificationsOff)

        else -> getIcon(constraintId)
    }

    fun getTitleStringId(constraintId: ConstraintId): Int = when (constraintId) {
        ConstraintId.APP_IN_FOREGROUND -> R.string.constraint_choose_app_foreground

        ConstraintId.APP_PLAYING_MEDIA -> R.string.constraint_choose_app_playing_media

        ConstraintId.MEDIA_PLAYING -> R.string.constraint_choose_media_playing

        ConstraintId.BT_DEVICE_CONNECTED -> R.string.constraint_choose_bluetooth_device_connected

        ConstraintId.SCREEN_ON -> R.string.constraint_choose_screen_on_description

        ConstraintId.DISPLAY_RESOLUTION -> R.string.constraint_choose_display_resolution

        ConstraintId.DISPLAY_ORIENTATION_PORTRAIT -> R.string.constraint_choose_orientation_portrait

        ConstraintId.DISPLAY_ORIENTATION_LANDSCAPE ->
            R.string.constraint_choose_orientation_landscape

        ConstraintId.DISPLAY_ORIENTATION_0 -> R.string.constraint_choose_orientation_0

        ConstraintId.DISPLAY_ORIENTATION_90 -> R.string.constraint_choose_orientation_90

        ConstraintId.DISPLAY_ORIENTATION_180 -> R.string.constraint_choose_orientation_180

        ConstraintId.DISPLAY_ORIENTATION_270 -> R.string.constraint_choose_orientation_270

        ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT ->
            R.string.constraint_choose_physical_orientation_portrait

        ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE ->
            R.string.constraint_choose_physical_orientation_landscape

        ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT_INVERTED ->
            R.string.constraint_choose_physical_orientation_portrait_inverted

        ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE_INVERTED ->
            R.string.constraint_choose_physical_orientation_landscape_inverted

        ConstraintId.FLASHLIGHT_ON -> R.string.constraint_flashlight_on

        ConstraintId.WIFI_ON -> R.string.constraint_wifi_on

        ConstraintId.WIFI_CONNECTED -> R.string.constraint_wifi_connected

        ConstraintId.IME_CHOSEN -> R.string.constraint_ime_chosen

        ConstraintId.KEYBOARD_SHOWING -> R.string.constraint_keyboard_showing

        ConstraintId.DEVICE_IS_LOCKED -> R.string.constraint_device_is_locked

        ConstraintId.IN_PHONE_CALL -> R.string.constraint_in_phone_call

        ConstraintId.NOT_IN_PHONE_CALL -> R.string.constraint_not_in_phone_call

        ConstraintId.PHONE_RINGING -> R.string.constraint_phone_ringing

        ConstraintId.RINGER_MODE_NORMAL -> R.string.constraint_ringer_mode_normal

        ConstraintId.RINGER_MODE_VIBRATE -> R.string.constraint_ringer_mode_vibrate

        ConstraintId.RINGER_MODE_SILENT -> R.string.constraint_ringer_mode_silent

        ConstraintId.CHARGING -> R.string.constraint_charging

        ConstraintId.HINGE_CLOSED -> R.string.constraint_hinge_closed

        ConstraintId.HINGE_OPEN -> R.string.constraint_hinge_open

        ConstraintId.LOCK_SCREEN_SHOWING -> R.string.constraint_lock_screen_showing

        ConstraintId.NOTIFICATION_PANEL_SHOWING ->
            R.string.constraint_notification_panel_showing

        ConstraintId.NOTIFICATION_POSTED -> R.string.constraint_notification_posted

        ConstraintId.TIME -> R.string.constraint_time
    }

    @StringRes
    fun getNotificationFieldLabel(field: NotificationField): Int = when (field) {
        NotificationField.PACKAGE -> R.string.notification_field_package_label
        NotificationField.TITLE -> R.string.notification_field_title_label
        NotificationField.TEXT -> R.string.notification_field_text_label
    }

    @StringRes
    fun getTextMatchModeLabel(matchMode: TextMatchMode): Int = when (matchMode) {
        TextMatchMode.CONTAINS -> R.string.notification_match_mode_contains
        TextMatchMode.EXACT -> R.string.notification_match_mode_exact
    }

    fun Constraint.getDependency(): Set<ConstraintDependency> {
        return when (data) {
            is ConstraintData.AppInForeground -> setOf(ConstraintDependency.FOREGROUND_APP)

            is ConstraintData.AppPlayingMedia -> setOf(ConstraintDependency.APP_PLAYING_MEDIA)

            is ConstraintData.BtDeviceConnected -> setOf(ConstraintDependency.CONNECTED_BT_DEVICES)

            is ConstraintData.Charging -> setOf(ConstraintDependency.CHARGING_STATE)

            is ConstraintData.DeviceIsLocked -> setOf(ConstraintDependency.DEVICE_LOCKED_STATE)

            is ConstraintData.FlashlightOn -> setOf(ConstraintDependency.FLASHLIGHT_STATE)

            is ConstraintData.ImeChosen -> setOf(ConstraintDependency.CHOSEN_IME)

            is ConstraintData.InPhoneCall,
            is ConstraintData.NotInPhoneCall,
            is ConstraintData.PhoneRinging,
                -> setOf(ConstraintDependency.PHONE_STATE)

            is ConstraintData.MediaPlaying -> setOf(ConstraintDependency.MEDIA_PLAYING)

            is ConstraintData.OrientationCustom,
            is ConstraintData.OrientationLandscape,
            is ConstraintData.OrientationPortrait,
                -> setOf(ConstraintDependency.DISPLAY_ORIENTATION)

            is ConstraintData.ScreenOn -> setOf(ConstraintDependency.SCREEN_STATE)

            is ConstraintData.WifiConnected -> setOf(ConstraintDependency.WIFI_SSID)

            is ConstraintData.WifiOn -> setOf(ConstraintDependency.WIFI_STATE)

            is ConstraintData.LockScreenShowing -> setOf(ConstraintDependency.LOCK_SCREEN_SHOWING)

            is ConstraintData.Time -> setOf(
                ConstraintDependency.FOREGROUND_APP,
                ConstraintDependency.SCREEN_STATE,
            )

            is ConstraintData.HingeClosed -> setOf(ConstraintDependency.HINGE_STATE)

            is ConstraintData.HingeOpen -> setOf(ConstraintDependency.HINGE_STATE)

            ConstraintData.KeyboardShowing -> setOf(ConstraintDependency.KEYBOARD_VISIBLE)

            is ConstraintData.PhysicalOrientation -> setOf(
                ConstraintDependency.PHYSICAL_ORIENTATION,
            )

            is ConstraintData.RingerMode -> setOf(ConstraintDependency.RINGER_MODE)

            ConstraintData.NotificationPanelShowing ->
                setOf(ConstraintDependency.NOTIFICATION_PANEL_STATE)

            is ConstraintData.DisplayResolution -> setOf(ConstraintDependency.DISPLAY_RESOLUTIONS)

            is ConstraintData.NotificationPosted ->
                setOf(ConstraintDependency.POSTED_NOTIFICATIONS)
        }
    }
}

fun ConstraintData.isEditable(): Boolean = when (this) {
    is ConstraintData.AppInForeground,
    is ConstraintData.AppPlayingMedia,
    is ConstraintData.BtDeviceConnected,
    is ConstraintData.OrientationCustom,
    is ConstraintData.PhysicalOrientation,
    is ConstraintData.DisplayResolution,
    is ConstraintData.FlashlightOn,
    is ConstraintData.WifiConnected,
    is ConstraintData.ImeChosen,
    is ConstraintData.NotificationPosted,
    is ConstraintData.Time,
        -> true

    else -> false
}
