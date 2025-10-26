package io.github.sds100.keymapper.base.constraints

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Battery2Bar
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MobileOff
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RingVolume
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.SignalWifiStatusbarNull
import androidx.compose.material.icons.outlined.StayCurrentLandscape
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.rounded.Android
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo

object ConstraintUtils {

    fun getIcon(constraintId: ConstraintId): ComposeIconInfo = when (constraintId) {
        ConstraintId.APP_IN_FOREGROUND,
        ConstraintId.APP_NOT_IN_FOREGROUND,
        ConstraintId.APP_PLAYING_MEDIA,
        ConstraintId.APP_NOT_PLAYING_MEDIA,
            -> ComposeIconInfo.Vector(Icons.Rounded.Android)

        ConstraintId.MEDIA_PLAYING -> ComposeIconInfo.Vector(Icons.Outlined.PlayArrow)
        ConstraintId.MEDIA_NOT_PLAYING -> ComposeIconInfo.Vector(Icons.Outlined.StopCircle)

        ConstraintId.BT_DEVICE_CONNECTED -> ComposeIconInfo.Vector(Icons.Outlined.BluetoothConnected)
        ConstraintId.BT_DEVICE_DISCONNECTED -> ComposeIconInfo.Vector(Icons.Outlined.BluetoothDisabled)

        ConstraintId.ORIENTATION_0,
        ConstraintId.ORIENTATION_180,
            -> ComposeIconInfo.Vector(Icons.Outlined.StayCurrentPortrait)

        ConstraintId.ORIENTATION_90,
        ConstraintId.ORIENTATION_270,
            -> ComposeIconInfo.Vector(Icons.Outlined.StayCurrentLandscape)

        ConstraintId.ORIENTATION_LANDSCAPE -> ComposeIconInfo.Vector(Icons.Outlined.StayCurrentLandscape)
        ConstraintId.ORIENTATION_PORTRAIT -> ComposeIconInfo.Vector(Icons.Outlined.StayCurrentPortrait)

        ConstraintId.SCREEN_OFF -> ComposeIconInfo.Vector(Icons.Outlined.MobileOff)
        ConstraintId.SCREEN_ON -> ComposeIconInfo.Vector(Icons.Outlined.StayCurrentPortrait)

        ConstraintId.FLASHLIGHT_OFF -> ComposeIconInfo.Vector(Icons.Outlined.FlashlightOff)
        ConstraintId.FLASHLIGHT_ON -> ComposeIconInfo.Vector(Icons.Outlined.FlashlightOn)

        ConstraintId.WIFI_CONNECTED -> ComposeIconInfo.Vector(Icons.Outlined.Wifi)
        ConstraintId.WIFI_DISCONNECTED -> ComposeIconInfo.Vector(Icons.Outlined.SignalWifiStatusbarNull)
        ConstraintId.WIFI_OFF -> ComposeIconInfo.Vector(Icons.Outlined.WifiOff)
        ConstraintId.WIFI_ON -> ComposeIconInfo.Vector(Icons.Outlined.Wifi)

        ConstraintId.IME_CHOSEN,
        ConstraintId.IME_NOT_CHOSEN,
            -> ComposeIconInfo.Vector(Icons.Outlined.Keyboard)

        ConstraintId.DEVICE_IS_LOCKED -> ComposeIconInfo.Vector(Icons.Outlined.Lock)
        ConstraintId.DEVICE_IS_UNLOCKED -> ComposeIconInfo.Vector(Icons.Outlined.LockOpen)

        ConstraintId.IN_PHONE_CALL -> ComposeIconInfo.Vector(Icons.Outlined.Call)
        ConstraintId.NOT_IN_PHONE_CALL -> ComposeIconInfo.Vector(Icons.Outlined.CallEnd)
        ConstraintId.PHONE_RINGING -> ComposeIconInfo.Vector(Icons.Outlined.RingVolume)

        ConstraintId.CHARGING -> ComposeIconInfo.Vector(Icons.Outlined.BatteryChargingFull)
        ConstraintId.DISCHARGING -> ComposeIconInfo.Vector(Icons.Outlined.Battery2Bar)
        
        ConstraintId.HINGE_CLOSED -> ComposeIconInfo.Vector(Icons.Outlined.StayCurrentPortrait)
        ConstraintId.HINGE_OPEN -> ComposeIconInfo.Vector(Icons.Outlined.StayCurrentLandscape)
        
        ConstraintId.LOCK_SCREEN_SHOWING -> ComposeIconInfo.Vector(Icons.Outlined.ScreenLockPortrait)
        ConstraintId.LOCK_SCREEN_NOT_SHOWING -> ComposeIconInfo.Vector(Icons.Outlined.LockOpen)
        ConstraintId.TIME -> ComposeIconInfo.Vector(Icons.Outlined.Timer)
    }

    fun getTitleStringId(constraintId: ConstraintId): Int = when (constraintId) {
        ConstraintId.APP_IN_FOREGROUND -> R.string.constraint_choose_app_foreground
        ConstraintId.APP_NOT_IN_FOREGROUND -> R.string.constraint_choose_app_not_foreground
        ConstraintId.APP_PLAYING_MEDIA -> R.string.constraint_choose_app_playing_media
        ConstraintId.APP_NOT_PLAYING_MEDIA -> R.string.constraint_choose_app_not_playing_media
        ConstraintId.MEDIA_NOT_PLAYING -> R.string.constraint_choose_media_not_playing
        ConstraintId.MEDIA_PLAYING -> R.string.constraint_choose_media_playing
        ConstraintId.BT_DEVICE_CONNECTED -> R.string.constraint_choose_bluetooth_device_connected
        ConstraintId.BT_DEVICE_DISCONNECTED -> R.string.constraint_choose_bluetooth_device_disconnected
        ConstraintId.SCREEN_ON -> R.string.constraint_choose_screen_on_description
        ConstraintId.SCREEN_OFF -> R.string.constraint_choose_screen_off_description
        ConstraintId.ORIENTATION_PORTRAIT -> R.string.constraint_choose_orientation_portrait
        ConstraintId.ORIENTATION_LANDSCAPE -> R.string.constraint_choose_orientation_landscape
        ConstraintId.ORIENTATION_0 -> R.string.constraint_choose_orientation_0
        ConstraintId.ORIENTATION_90 -> R.string.constraint_choose_orientation_90
        ConstraintId.ORIENTATION_180 -> R.string.constraint_choose_orientation_180
        ConstraintId.ORIENTATION_270 -> R.string.constraint_choose_orientation_270
        ConstraintId.FLASHLIGHT_ON -> R.string.constraint_flashlight_on
        ConstraintId.FLASHLIGHT_OFF -> R.string.constraint_flashlight_off
        ConstraintId.WIFI_ON -> R.string.constraint_wifi_on
        ConstraintId.WIFI_OFF -> R.string.constraint_wifi_off
        ConstraintId.WIFI_CONNECTED -> R.string.constraint_wifi_connected
        ConstraintId.WIFI_DISCONNECTED -> R.string.constraint_wifi_disconnected
        ConstraintId.IME_CHOSEN -> R.string.constraint_ime_chosen
        ConstraintId.IME_NOT_CHOSEN -> R.string.constraint_ime_not_chosen
        ConstraintId.DEVICE_IS_LOCKED -> R.string.constraint_device_is_locked
        ConstraintId.DEVICE_IS_UNLOCKED -> R.string.constraint_device_is_unlocked
        ConstraintId.IN_PHONE_CALL -> R.string.constraint_in_phone_call
        ConstraintId.NOT_IN_PHONE_CALL -> R.string.constraint_not_in_phone_call
        ConstraintId.PHONE_RINGING -> R.string.constraint_phone_ringing
        ConstraintId.CHARGING -> R.string.constraint_charging
        ConstraintId.DISCHARGING -> R.string.constraint_discharging
        ConstraintId.HINGE_CLOSED -> R.string.constraint_hinge_closed
        ConstraintId.HINGE_OPEN -> R.string.constraint_hinge_open
        ConstraintId.LOCK_SCREEN_SHOWING -> R.string.constraint_lock_screen_showing
        ConstraintId.LOCK_SCREEN_NOT_SHOWING -> R.string.constraint_lock_screen_not_showing
        ConstraintId.TIME -> R.string.constraint_time
    }
}
