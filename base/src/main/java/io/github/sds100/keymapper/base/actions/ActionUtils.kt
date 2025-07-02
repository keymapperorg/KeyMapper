package io.github.sds100.keymapper.base.actions

import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeMute
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AirplanemodeActive
import androidx.compose.material.icons.outlined.AirplanemodeInactive
import androidx.compose.material.icons.outlined.Assistant
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.DoNotDisturb
import androidx.compose.material.icons.outlined.DoNotDisturbOff
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Forward30
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardHide
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.NotStarted
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhonelinkRing
import androidx.compose.material.icons.outlined.Pinch
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Replay30
import androidx.compose.material.icons.outlined.ScreenLockRotation
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SignalCellular4Bar
import androidx.compose.material.icons.outlined.SignalCellularOff
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.material.icons.outlined.StayCurrentLandscape
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.ViewArray
import androidx.compose.material.icons.rounded.Abc
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.sds100.keymapper.base.Constants
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.compose.icons.HomeIotDevice
import io.github.sds100.keymapper.base.utils.ui.compose.icons.InstantMix
import io.github.sds100.keymapper.base.utils.ui.compose.icons.JumpToElement
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.utils.ui.compose.icons.MatchWord
import io.github.sds100.keymapper.base.utils.ui.compose.icons.NfcOff
import io.github.sds100.keymapper.base.utils.ui.compose.icons.TextSelectEnd
import io.github.sds100.keymapper.base.utils.ui.compose.icons.TopPanelClose
import io.github.sds100.keymapper.base.utils.ui.compose.icons.TopPanelOpen
import io.github.sds100.keymapper.system.permissions.Permission

object ActionUtils {

    @StringRes
    fun getCategoryLabel(category: ActionCategory): Int = when (category) {
        ActionCategory.NAVIGATION -> R.string.action_cat_navigation
        ActionCategory.VOLUME -> R.string.action_cat_volume
        ActionCategory.MEDIA -> R.string.action_cat_media
        ActionCategory.KEYBOARD -> R.string.action_cat_keyboard
        ActionCategory.APPS -> R.string.action_cat_apps
        ActionCategory.INPUT -> R.string.action_cat_input
        ActionCategory.FLASHLIGHT -> R.string.action_cat_flashlight
        ActionCategory.CONNECTIVITY -> R.string.action_cat_connectivity
        ActionCategory.CONTENT -> R.string.action_cat_content
        ActionCategory.INTERFACE -> R.string.action_cat_interface
        ActionCategory.TELEPHONY -> R.string.action_cat_telephony
        ActionCategory.DISPLAY -> R.string.action_cat_display
        ActionCategory.NOTIFICATIONS -> R.string.action_cat_notifications
        ActionCategory.SPECIAL -> R.string.action_cat_special
    }

    fun getCategory(id: ActionId): ActionCategory = when (id) {
        ActionId.KEY_CODE -> ActionCategory.INPUT
        ActionId.KEY_EVENT -> ActionCategory.INPUT
        ActionId.TAP_SCREEN -> ActionCategory.INPUT
        ActionId.SWIPE_SCREEN -> ActionCategory.INPUT
        ActionId.PINCH_SCREEN -> ActionCategory.INPUT
        ActionId.TEXT -> ActionCategory.INPUT

        ActionId.OPEN_VOICE_ASSISTANT -> ActionCategory.APPS
        ActionId.OPEN_DEVICE_ASSISTANT -> ActionCategory.APPS
        ActionId.OPEN_CAMERA -> ActionCategory.APPS
        ActionId.OPEN_SETTINGS -> ActionCategory.APPS
        ActionId.APP -> ActionCategory.APPS
        ActionId.APP_SHORTCUT -> ActionCategory.APPS
        ActionId.INTENT -> ActionCategory.APPS

        ActionId.TOGGLE_WIFI -> ActionCategory.CONNECTIVITY
        ActionId.ENABLE_WIFI -> ActionCategory.CONNECTIVITY
        ActionId.DISABLE_WIFI -> ActionCategory.CONNECTIVITY

        ActionId.TOGGLE_BLUETOOTH -> ActionCategory.CONNECTIVITY
        ActionId.ENABLE_BLUETOOTH -> ActionCategory.CONNECTIVITY
        ActionId.DISABLE_BLUETOOTH -> ActionCategory.CONNECTIVITY

        ActionId.TOGGLE_MOBILE_DATA -> ActionCategory.CONNECTIVITY
        ActionId.ENABLE_MOBILE_DATA -> ActionCategory.CONNECTIVITY
        ActionId.DISABLE_MOBILE_DATA -> ActionCategory.CONNECTIVITY

        ActionId.TOGGLE_AUTO_BRIGHTNESS -> ActionCategory.DISPLAY
        ActionId.DISABLE_AUTO_BRIGHTNESS -> ActionCategory.DISPLAY
        ActionId.ENABLE_AUTO_BRIGHTNESS -> ActionCategory.DISPLAY
        ActionId.INCREASE_BRIGHTNESS -> ActionCategory.DISPLAY
        ActionId.DECREASE_BRIGHTNESS -> ActionCategory.DISPLAY

        ActionId.TOGGLE_AUTO_ROTATE -> ActionCategory.INTERFACE
        ActionId.ENABLE_AUTO_ROTATE -> ActionCategory.INTERFACE
        ActionId.DISABLE_AUTO_ROTATE -> ActionCategory.INTERFACE
        ActionId.PORTRAIT_MODE -> ActionCategory.INTERFACE
        ActionId.LANDSCAPE_MODE -> ActionCategory.INTERFACE
        ActionId.SWITCH_ORIENTATION -> ActionCategory.INTERFACE
        ActionId.CYCLE_ROTATIONS -> ActionCategory.INTERFACE

        ActionId.VOLUME_UP -> ActionCategory.VOLUME
        ActionId.VOLUME_DOWN -> ActionCategory.VOLUME
        ActionId.VOLUME_SHOW_DIALOG -> ActionCategory.VOLUME
        ActionId.VOLUME_DECREASE_STREAM -> ActionCategory.VOLUME
        ActionId.VOLUME_INCREASE_STREAM -> ActionCategory.VOLUME
        ActionId.CYCLE_RINGER_MODE -> ActionCategory.VOLUME
        ActionId.CHANGE_RINGER_MODE -> ActionCategory.VOLUME
        ActionId.CYCLE_VIBRATE_RING -> ActionCategory.VOLUME
        ActionId.TOGGLE_DND_MODE -> ActionCategory.VOLUME
        ActionId.ENABLE_DND_MODE -> ActionCategory.VOLUME
        ActionId.DISABLE_DND_MODE -> ActionCategory.VOLUME
        ActionId.VOLUME_UNMUTE -> ActionCategory.VOLUME
        ActionId.VOLUME_MUTE -> ActionCategory.VOLUME
        ActionId.VOLUME_TOGGLE_MUTE -> ActionCategory.VOLUME

        ActionId.EXPAND_NOTIFICATION_DRAWER -> ActionCategory.NAVIGATION
        ActionId.TOGGLE_NOTIFICATION_DRAWER -> ActionCategory.NAVIGATION
        ActionId.EXPAND_QUICK_SETTINGS -> ActionCategory.NAVIGATION
        ActionId.TOGGLE_QUICK_SETTINGS -> ActionCategory.NAVIGATION
        ActionId.COLLAPSE_STATUS_BAR -> ActionCategory.NAVIGATION

        ActionId.SOUND -> ActionCategory.MEDIA
        ActionId.PAUSE_MEDIA -> ActionCategory.MEDIA
        ActionId.PAUSE_MEDIA_PACKAGE -> ActionCategory.MEDIA
        ActionId.PLAY_MEDIA -> ActionCategory.MEDIA
        ActionId.PLAY_MEDIA_PACKAGE -> ActionCategory.MEDIA
        ActionId.PLAY_PAUSE_MEDIA -> ActionCategory.MEDIA
        ActionId.PLAY_PAUSE_MEDIA_PACKAGE -> ActionCategory.MEDIA
        ActionId.NEXT_TRACK -> ActionCategory.MEDIA
        ActionId.NEXT_TRACK_PACKAGE -> ActionCategory.MEDIA
        ActionId.PREVIOUS_TRACK -> ActionCategory.MEDIA
        ActionId.PREVIOUS_TRACK_PACKAGE -> ActionCategory.MEDIA
        ActionId.FAST_FORWARD -> ActionCategory.MEDIA
        ActionId.FAST_FORWARD_PACKAGE -> ActionCategory.MEDIA
        ActionId.REWIND -> ActionCategory.MEDIA
        ActionId.REWIND_PACKAGE -> ActionCategory.MEDIA
        ActionId.STOP_MEDIA -> ActionCategory.MEDIA
        ActionId.STOP_MEDIA_PACKAGE -> ActionCategory.MEDIA
        ActionId.STEP_FORWARD -> ActionCategory.MEDIA
        ActionId.STEP_FORWARD_PACKAGE -> ActionCategory.MEDIA
        ActionId.STEP_BACKWARD -> ActionCategory.MEDIA
        ActionId.STEP_BACKWARD_PACKAGE -> ActionCategory.MEDIA

        ActionId.GO_BACK -> ActionCategory.NAVIGATION
        ActionId.GO_HOME -> ActionCategory.NAVIGATION
        ActionId.OPEN_RECENTS -> ActionCategory.NAVIGATION
        ActionId.TOGGLE_SPLIT_SCREEN -> ActionCategory.NAVIGATION
        ActionId.GO_LAST_APP -> ActionCategory.NAVIGATION
        ActionId.OPEN_MENU -> ActionCategory.NAVIGATION

        ActionId.TOGGLE_FLASHLIGHT -> ActionCategory.FLASHLIGHT
        ActionId.ENABLE_FLASHLIGHT -> ActionCategory.FLASHLIGHT
        ActionId.DISABLE_FLASHLIGHT -> ActionCategory.FLASHLIGHT
        ActionId.CHANGE_FLASHLIGHT_STRENGTH -> ActionCategory.FLASHLIGHT

        ActionId.ENABLE_NFC -> ActionCategory.CONNECTIVITY
        ActionId.DISABLE_NFC -> ActionCategory.CONNECTIVITY
        ActionId.TOGGLE_NFC -> ActionCategory.CONNECTIVITY

        ActionId.TOGGLE_AIRPLANE_MODE -> ActionCategory.CONNECTIVITY
        ActionId.ENABLE_AIRPLANE_MODE -> ActionCategory.CONNECTIVITY
        ActionId.DISABLE_AIRPLANE_MODE -> ActionCategory.CONNECTIVITY

        ActionId.MOVE_CURSOR_TO_END -> ActionCategory.KEYBOARD
        ActionId.TOGGLE_KEYBOARD -> ActionCategory.KEYBOARD
        ActionId.SHOW_KEYBOARD -> ActionCategory.KEYBOARD
        ActionId.HIDE_KEYBOARD -> ActionCategory.KEYBOARD
        ActionId.SHOW_KEYBOARD_PICKER -> ActionCategory.KEYBOARD
        ActionId.SELECT_WORD_AT_CURSOR -> ActionCategory.KEYBOARD
        ActionId.SWITCH_KEYBOARD -> ActionCategory.KEYBOARD

        ActionId.LOCK_DEVICE -> ActionCategory.INTERFACE
        ActionId.POWER_ON_OFF_DEVICE -> ActionCategory.INTERFACE
        ActionId.SECURE_LOCK_DEVICE -> ActionCategory.INTERFACE
        ActionId.SHOW_POWER_MENU -> ActionCategory.INTERFACE

        ActionId.TEXT_CUT -> ActionCategory.CONTENT
        ActionId.TEXT_COPY -> ActionCategory.CONTENT
        ActionId.TEXT_PASTE -> ActionCategory.CONTENT
        ActionId.SCREENSHOT -> ActionCategory.CONTENT
        ActionId.URL -> ActionCategory.CONTENT
        ActionId.HTTP_REQUEST -> ActionCategory.CONTENT

        ActionId.PHONE_CALL -> ActionCategory.TELEPHONY
        ActionId.ANSWER_PHONE_CALL -> ActionCategory.TELEPHONY
        ActionId.END_PHONE_CALL -> ActionCategory.TELEPHONY

        ActionId.DISMISS_MOST_RECENT_NOTIFICATION -> ActionCategory.NOTIFICATIONS
        ActionId.DISMISS_ALL_NOTIFICATIONS -> ActionCategory.NOTIFICATIONS
        ActionId.DEVICE_CONTROLS -> ActionCategory.APPS

        ActionId.INTERACT_UI_ELEMENT -> ActionCategory.APPS

        ActionId.CONSUME_KEY_EVENT -> ActionCategory.SPECIAL
    }

    @StringRes
    fun getTitle(id: ActionId): Int = when (id) {
        ActionId.TOGGLE_WIFI -> R.string.action_toggle_wifi
        ActionId.ENABLE_WIFI -> R.string.action_enable_wifi
        ActionId.DISABLE_WIFI -> R.string.action_disable_wifi
        ActionId.TOGGLE_BLUETOOTH -> R.string.action_toggle_bluetooth
        ActionId.ENABLE_BLUETOOTH -> R.string.action_enable_bluetooth
        ActionId.DISABLE_BLUETOOTH -> R.string.action_disable_bluetooth
        ActionId.TOGGLE_MOBILE_DATA -> R.string.action_toggle_mobile_data
        ActionId.ENABLE_MOBILE_DATA -> R.string.action_enable_mobile_data
        ActionId.DISABLE_MOBILE_DATA -> R.string.action_disable_mobile_data
        ActionId.TOGGLE_AUTO_BRIGHTNESS -> R.string.action_toggle_auto_brightness
        ActionId.DISABLE_AUTO_BRIGHTNESS -> R.string.action_disable_auto_brightness
        ActionId.ENABLE_AUTO_BRIGHTNESS -> R.string.action_enable_auto_brightness
        ActionId.INCREASE_BRIGHTNESS -> R.string.action_increase_brightness
        ActionId.DECREASE_BRIGHTNESS -> R.string.action_decrease_brightness
        ActionId.TOGGLE_AUTO_ROTATE -> R.string.action_toggle_auto_rotate
        ActionId.ENABLE_AUTO_ROTATE -> R.string.action_enable_auto_rotate
        ActionId.DISABLE_AUTO_ROTATE -> R.string.action_disable_auto_rotate
        ActionId.PORTRAIT_MODE -> R.string.action_portrait_mode
        ActionId.LANDSCAPE_MODE -> R.string.action_landscape_mode
        ActionId.SWITCH_ORIENTATION -> R.string.action_switch_orientation
        ActionId.CYCLE_ROTATIONS -> R.string.action_cycle_rotations
        ActionId.VOLUME_UP -> R.string.action_volume_up
        ActionId.VOLUME_DOWN -> R.string.action_volume_down
        ActionId.VOLUME_SHOW_DIALOG -> R.string.action_volume_show_dialog
        ActionId.VOLUME_DECREASE_STREAM -> R.string.action_decrease_stream
        ActionId.VOLUME_INCREASE_STREAM -> R.string.action_increase_stream
        ActionId.CYCLE_RINGER_MODE -> R.string.action_cycle_ringer_mode
        ActionId.CHANGE_RINGER_MODE -> R.string.action_change_ringer_mode
        ActionId.CYCLE_VIBRATE_RING -> R.string.action_cycle_vibrate_ring
        ActionId.TOGGLE_DND_MODE -> R.string.action_toggle_dnd_mode
        ActionId.ENABLE_DND_MODE -> R.string.action_enable_dnd_mode
        ActionId.DISABLE_DND_MODE -> R.string.action_disable_dnd_mode
        ActionId.VOLUME_UNMUTE -> R.string.action_volume_unmute
        ActionId.VOLUME_MUTE -> R.string.action_volume_mute
        ActionId.VOLUME_TOGGLE_MUTE -> R.string.action_toggle_mute
        ActionId.EXPAND_NOTIFICATION_DRAWER -> R.string.action_expand_notification_drawer
        ActionId.TOGGLE_NOTIFICATION_DRAWER -> R.string.action_toggle_notification_drawer
        ActionId.EXPAND_QUICK_SETTINGS -> R.string.action_expand_quick_settings
        ActionId.TOGGLE_QUICK_SETTINGS -> R.string.action_toggle_quick_settings
        ActionId.COLLAPSE_STATUS_BAR -> R.string.action_collapse_status_bar
        ActionId.PAUSE_MEDIA -> R.string.action_pause_media
        ActionId.PAUSE_MEDIA_PACKAGE -> R.string.action_pause_media_package
        ActionId.PLAY_MEDIA -> R.string.action_play_media
        ActionId.PLAY_MEDIA_PACKAGE -> R.string.action_play_media_package
        ActionId.PLAY_PAUSE_MEDIA -> R.string.action_play_pause_media
        ActionId.PLAY_PAUSE_MEDIA_PACKAGE -> R.string.action_play_pause_media_package
        ActionId.NEXT_TRACK -> R.string.action_next_track
        ActionId.NEXT_TRACK_PACKAGE -> R.string.action_next_track_package
        ActionId.PREVIOUS_TRACK -> R.string.action_previous_track
        ActionId.PREVIOUS_TRACK_PACKAGE -> R.string.action_previous_track_package
        ActionId.FAST_FORWARD -> R.string.action_fast_forward
        ActionId.FAST_FORWARD_PACKAGE -> R.string.action_fast_forward_package
        ActionId.REWIND -> R.string.action_rewind
        ActionId.REWIND_PACKAGE -> R.string.action_rewind_package
        ActionId.STOP_MEDIA -> R.string.action_stop_media
        ActionId.STOP_MEDIA_PACKAGE -> R.string.action_stop_media_package
        ActionId.STEP_FORWARD -> R.string.action_step_forward_media
        ActionId.STEP_FORWARD_PACKAGE -> R.string.action_step_forward_media_package
        ActionId.STEP_BACKWARD -> R.string.action_step_backward_media
        ActionId.STEP_BACKWARD_PACKAGE -> R.string.action_step_backward_media_package
        ActionId.GO_BACK -> R.string.action_go_back
        ActionId.GO_HOME -> R.string.action_go_home
        ActionId.OPEN_RECENTS -> R.string.action_open_recents
        ActionId.TOGGLE_SPLIT_SCREEN -> R.string.action_toggle_split_screen
        ActionId.GO_LAST_APP -> R.string.action_go_last_app
        ActionId.OPEN_MENU -> R.string.action_open_menu
        ActionId.TOGGLE_FLASHLIGHT -> R.string.action_toggle_flashlight
        ActionId.ENABLE_FLASHLIGHT -> R.string.action_enable_flashlight
        ActionId.DISABLE_FLASHLIGHT -> R.string.action_disable_flashlight
        ActionId.CHANGE_FLASHLIGHT_STRENGTH -> R.string.action_flashlight_change_strength
        ActionId.ENABLE_NFC -> R.string.action_nfc_enable
        ActionId.DISABLE_NFC -> R.string.action_nfc_disable
        ActionId.TOGGLE_NFC -> R.string.action_nfc_toggle
        ActionId.MOVE_CURSOR_TO_END -> R.string.action_move_to_end_of_text
        ActionId.TOGGLE_KEYBOARD -> R.string.action_toggle_keyboard
        ActionId.SHOW_KEYBOARD -> R.string.action_show_keyboard
        ActionId.HIDE_KEYBOARD -> R.string.action_hide_keyboard
        ActionId.SHOW_KEYBOARD_PICKER -> R.string.action_show_keyboard_picker
        ActionId.TEXT_CUT -> R.string.action_text_cut
        ActionId.TEXT_COPY -> R.string.action_text_copy
        ActionId.TEXT_PASTE -> R.string.action_text_paste
        ActionId.SELECT_WORD_AT_CURSOR -> R.string.action_select_word_at_cursor
        ActionId.SWITCH_KEYBOARD -> R.string.action_switch_keyboard
        ActionId.TOGGLE_AIRPLANE_MODE -> R.string.action_toggle_airplane_mode
        ActionId.ENABLE_AIRPLANE_MODE -> R.string.action_enable_airplane_mode
        ActionId.DISABLE_AIRPLANE_MODE -> R.string.action_disable_airplane_mode
        ActionId.SCREENSHOT -> R.string.action_screenshot
        ActionId.OPEN_VOICE_ASSISTANT -> R.string.action_open_assistant
        ActionId.OPEN_DEVICE_ASSISTANT -> R.string.action_open_device_assistant
        ActionId.OPEN_CAMERA -> R.string.action_open_camera
        ActionId.LOCK_DEVICE -> R.string.action_lock_device
        ActionId.POWER_ON_OFF_DEVICE -> R.string.action_power_on_off_device
        ActionId.SECURE_LOCK_DEVICE -> R.string.action_secure_lock_device
        ActionId.CONSUME_KEY_EVENT -> R.string.action_consume_keyevent
        ActionId.OPEN_SETTINGS -> R.string.action_open_settings
        ActionId.SHOW_POWER_MENU -> R.string.action_show_power_menu
        ActionId.APP -> R.string.action_open_app
        ActionId.APP_SHORTCUT -> R.string.action_open_app_shortcut
        ActionId.KEY_CODE -> R.string.action_input_key_code
        ActionId.KEY_EVENT -> R.string.action_input_key_event
        ActionId.TAP_SCREEN -> R.string.action_tap_screen
        ActionId.SWIPE_SCREEN -> R.string.action_swipe_screen
        ActionId.PINCH_SCREEN -> R.string.action_pinch_screen
        ActionId.TEXT -> R.string.action_input_text
        ActionId.URL -> R.string.action_open_url
        ActionId.INTENT -> R.string.action_send_intent
        ActionId.PHONE_CALL -> R.string.action_phone_call
        ActionId.SOUND -> R.string.action_play_sound
        ActionId.DISMISS_MOST_RECENT_NOTIFICATION -> R.string.action_dismiss_most_recent_notification
        ActionId.DISMISS_ALL_NOTIFICATIONS -> R.string.action_dismiss_all_notifications
        ActionId.ANSWER_PHONE_CALL -> R.string.action_answer_call
        ActionId.END_PHONE_CALL -> R.string.action_end_call
        ActionId.DEVICE_CONTROLS -> R.string.action_device_controls
        ActionId.HTTP_REQUEST -> R.string.action_http_request
        ActionId.INTERACT_UI_ELEMENT -> R.string.action_interact_ui_element_title
    }

    @DrawableRes
    fun getDrawableIcon(id: ActionId): Int? = when (id) {
        ActionId.TOGGLE_WIFI -> R.drawable.ic_outline_wifi_24
        ActionId.ENABLE_WIFI -> R.drawable.ic_outline_wifi_24
        ActionId.DISABLE_WIFI -> R.drawable.ic_outline_wifi_off_24
        ActionId.TOGGLE_BLUETOOTH -> R.drawable.ic_outline_bluetooth_24
        ActionId.ENABLE_BLUETOOTH -> R.drawable.ic_outline_bluetooth_24
        ActionId.DISABLE_BLUETOOTH -> R.drawable.ic_outline_bluetooth_disabled_24
        ActionId.TOGGLE_MOBILE_DATA -> R.drawable.ic_outline_signal_cellular_4_bar_24
        ActionId.ENABLE_MOBILE_DATA -> R.drawable.ic_outline_signal_cellular_4_bar_24
        ActionId.DISABLE_MOBILE_DATA -> R.drawable.ic_outline_signal_cellular_off_24
        ActionId.TOGGLE_AUTO_BRIGHTNESS -> R.drawable.ic_outline_brightness_auto_24
        ActionId.DISABLE_AUTO_BRIGHTNESS -> R.drawable.ic_disable_brightness_auto_24dp
        ActionId.ENABLE_AUTO_BRIGHTNESS -> R.drawable.ic_outline_brightness_auto_24
        ActionId.INCREASE_BRIGHTNESS -> R.drawable.ic_outline_brightness_high_24
        ActionId.DECREASE_BRIGHTNESS -> R.drawable.ic_outline_brightness_low_24
        ActionId.TOGGLE_AUTO_ROTATE -> R.drawable.ic_outline_screen_rotation_24
        ActionId.ENABLE_AUTO_ROTATE -> R.drawable.ic_outline_screen_rotation_24
        ActionId.DISABLE_AUTO_ROTATE -> R.drawable.ic_outline_screen_lock_rotation_24
        ActionId.PORTRAIT_MODE -> R.drawable.ic_outline_stay_current_portrait_24
        ActionId.LANDSCAPE_MODE -> R.drawable.ic_outline_stay_current_landscape_24
        ActionId.SWITCH_ORIENTATION -> R.drawable.ic_outline_screen_rotation_24
        ActionId.CYCLE_ROTATIONS -> R.drawable.ic_outline_screen_rotation_24
        ActionId.VOLUME_UP -> R.drawable.ic_outline_volume_up_24
        ActionId.VOLUME_DOWN -> R.drawable.ic_outline_volume_down_24
        ActionId.VOLUME_SHOW_DIALOG -> null
        ActionId.VOLUME_DECREASE_STREAM -> R.drawable.ic_outline_volume_down_24
        ActionId.VOLUME_INCREASE_STREAM -> R.drawable.ic_outline_volume_up_24
        ActionId.CYCLE_RINGER_MODE -> null
        ActionId.CHANGE_RINGER_MODE -> null
        ActionId.CYCLE_VIBRATE_RING -> null
        ActionId.TOGGLE_DND_MODE -> R.drawable.dnd_circle_outline
        ActionId.ENABLE_DND_MODE -> R.drawable.dnd_circle_outline
        ActionId.DISABLE_DND_MODE -> R.drawable.dnd_circle_off_outline
        ActionId.VOLUME_UNMUTE -> R.drawable.ic_outline_volume_up_24
        ActionId.VOLUME_MUTE -> R.drawable.ic_outline_volume_mute_24
        ActionId.VOLUME_TOGGLE_MUTE -> R.drawable.ic_outline_volume_mute_24
        ActionId.EXPAND_NOTIFICATION_DRAWER -> null
        ActionId.TOGGLE_NOTIFICATION_DRAWER -> null
        ActionId.EXPAND_QUICK_SETTINGS -> null
        ActionId.TOGGLE_QUICK_SETTINGS -> null
        ActionId.COLLAPSE_STATUS_BAR -> null
        ActionId.PAUSE_MEDIA -> R.drawable.ic_outline_pause_24
        ActionId.PAUSE_MEDIA_PACKAGE -> R.drawable.ic_outline_pause_24
        ActionId.PLAY_MEDIA -> R.drawable.ic_outline_play_arrow_24
        ActionId.PLAY_MEDIA_PACKAGE -> R.drawable.ic_outline_play_arrow_24
        ActionId.PLAY_PAUSE_MEDIA -> R.drawable.ic_play_pause_24dp
        ActionId.PLAY_PAUSE_MEDIA_PACKAGE -> R.drawable.ic_play_pause_24dp
        ActionId.NEXT_TRACK -> R.drawable.ic_outline_skip_next_24
        ActionId.NEXT_TRACK_PACKAGE -> R.drawable.ic_outline_skip_next_24
        ActionId.PREVIOUS_TRACK -> R.drawable.ic_outline_skip_previous_24
        ActionId.PREVIOUS_TRACK_PACKAGE -> R.drawable.ic_outline_skip_previous_24
        ActionId.FAST_FORWARD -> R.drawable.ic_outline_fast_forward_24
        ActionId.FAST_FORWARD_PACKAGE -> R.drawable.ic_outline_fast_forward_24
        ActionId.REWIND -> R.drawable.ic_outline_fast_rewind_24
        ActionId.REWIND_PACKAGE -> R.drawable.ic_outline_fast_rewind_24
        ActionId.STOP_MEDIA -> R.drawable.ic_outline_pause_24
        ActionId.STOP_MEDIA_PACKAGE -> R.drawable.ic_outline_pause_24
        ActionId.STEP_FORWARD -> null
        ActionId.STEP_FORWARD_PACKAGE -> null
        ActionId.STEP_BACKWARD -> null
        ActionId.STEP_BACKWARD_PACKAGE -> null
        ActionId.GO_BACK -> R.drawable.ic_baseline_arrow_back_24
        ActionId.GO_HOME -> R.drawable.ic_outline_home_24
        ActionId.OPEN_RECENTS -> null
        ActionId.TOGGLE_SPLIT_SCREEN -> null
        ActionId.GO_LAST_APP -> null
        ActionId.OPEN_MENU -> R.drawable.ic_outline_more_vert_24
        ActionId.TOGGLE_FLASHLIGHT -> R.drawable.ic_flashlight
        ActionId.ENABLE_FLASHLIGHT -> R.drawable.ic_flashlight
        ActionId.DISABLE_FLASHLIGHT -> R.drawable.ic_flashlight_off
        ActionId.CHANGE_FLASHLIGHT_STRENGTH -> R.drawable.ic_flashlight
        ActionId.ENABLE_NFC -> R.drawable.ic_outline_nfc_24
        ActionId.DISABLE_NFC -> R.drawable.ic_nfc_off
        ActionId.TOGGLE_NFC -> R.drawable.ic_outline_nfc_24
        ActionId.MOVE_CURSOR_TO_END -> R.drawable.ic_cursor
        ActionId.TOGGLE_KEYBOARD -> R.drawable.ic_outline_keyboard_24
        ActionId.SHOW_KEYBOARD -> R.drawable.ic_outline_keyboard_24
        ActionId.HIDE_KEYBOARD -> R.drawable.ic_outline_keyboard_hide_24
        ActionId.SHOW_KEYBOARD_PICKER -> R.drawable.ic_outline_keyboard_24
        ActionId.TEXT_CUT -> R.drawable.ic_content_cut
        ActionId.TEXT_COPY -> R.drawable.ic_content_copy
        ActionId.TEXT_PASTE -> R.drawable.ic_content_paste
        ActionId.SELECT_WORD_AT_CURSOR -> null
        ActionId.SWITCH_KEYBOARD -> R.drawable.ic_outline_keyboard_24
        ActionId.TOGGLE_AIRPLANE_MODE -> R.drawable.ic_outline_airplanemode_active_24
        ActionId.ENABLE_AIRPLANE_MODE -> R.drawable.ic_outline_airplanemode_active_24
        ActionId.DISABLE_AIRPLANE_MODE -> R.drawable.ic_outline_airplanemode_inactive_24
        ActionId.SCREENSHOT -> R.drawable.ic_outline_fullscreen_24
        ActionId.OPEN_VOICE_ASSISTANT -> R.drawable.ic_outline_assistant_24
        ActionId.OPEN_DEVICE_ASSISTANT -> R.drawable.ic_outline_assistant_24
        ActionId.OPEN_CAMERA -> R.drawable.ic_outline_camera_alt_24
        ActionId.LOCK_DEVICE -> R.drawable.ic_outline_lock_24
        ActionId.POWER_ON_OFF_DEVICE -> R.drawable.ic_outline_power_settings_new_24
        ActionId.SECURE_LOCK_DEVICE -> R.drawable.ic_outline_lock_24
        ActionId.CONSUME_KEY_EVENT -> null
        ActionId.OPEN_SETTINGS -> R.drawable.ic_outline_settings_24
        ActionId.SHOW_POWER_MENU -> R.drawable.ic_outline_power_settings_new_24
        ActionId.APP -> R.drawable.ic_outline_android_24
        ActionId.APP_SHORTCUT -> R.drawable.ic_outline_open_in_new_24
        ActionId.KEY_CODE -> R.drawable.ic_q_24
        ActionId.KEY_EVENT -> R.drawable.ic_q_24
        ActionId.TAP_SCREEN -> R.drawable.ic_outline_touch_app_24
        ActionId.SWIPE_SCREEN -> R.drawable.ic_outline_swipe_app_24
        ActionId.PINCH_SCREEN -> R.drawable.ic_outline_pinch_app_24
        ActionId.TEXT -> R.drawable.ic_outline_short_text_24
        ActionId.URL -> R.drawable.ic_outline_link_24
        ActionId.INTENT -> null
        ActionId.PHONE_CALL -> R.drawable.ic_outline_call_24
        ActionId.SOUND -> R.drawable.ic_outline_volume_up_24
        ActionId.DISMISS_MOST_RECENT_NOTIFICATION -> R.drawable.ic_baseline_clear_all_24
        ActionId.DISMISS_ALL_NOTIFICATIONS -> R.drawable.ic_baseline_clear_all_24
        ActionId.ANSWER_PHONE_CALL -> R.drawable.ic_outline_call_24
        ActionId.END_PHONE_CALL -> R.drawable.ic_outline_call_end_24
        ActionId.DEVICE_CONTROLS -> R.drawable.ic_home_automation
        ActionId.HTTP_REQUEST -> null
        ActionId.INTERACT_UI_ELEMENT -> null
    }

    fun getMinApi(id: ActionId): Int = when (id) {
        ActionId.ANSWER_PHONE_CALL -> Build.VERSION_CODES.O
        ActionId.END_PHONE_CALL -> Build.VERSION_CODES.P

        ActionId.TOGGLE_SPLIT_SCREEN -> Build.VERSION_CODES.N
        ActionId.GO_LAST_APP -> Build.VERSION_CODES.N
        ActionId.TAP_SCREEN -> Build.VERSION_CODES.N

        ActionId.VOLUME_MUTE,
        ActionId.VOLUME_UNMUTE,
        ActionId.VOLUME_TOGGLE_MUTE,
        ActionId.TOGGLE_DND_MODE,
        ActionId.ENABLE_DND_MODE,
        ActionId.DISABLE_DND_MODE,
        -> Build.VERSION_CODES.M

        ActionId.DISABLE_FLASHLIGHT,
        ActionId.ENABLE_FLASHLIGHT,
        ActionId.TOGGLE_FLASHLIGHT,
        -> Build.VERSION_CODES.M

        ActionId.CHANGE_FLASHLIGHT_STRENGTH,
        -> Build.VERSION_CODES.TIRAMISU

        ActionId.TOGGLE_KEYBOARD,
        ActionId.SHOW_KEYBOARD,
        ActionId.HIDE_KEYBOARD,
        -> Build.VERSION_CODES.N

        ActionId.TEXT_CUT,
        ActionId.TEXT_COPY,
        ActionId.TEXT_PASTE,
        ActionId.SELECT_WORD_AT_CURSOR,
        -> Build.VERSION_CODES.JELLY_BEAN_MR2

        ActionId.SHOW_POWER_MENU -> Build.VERSION_CODES.LOLLIPOP
        ActionId.DEVICE_CONTROLS -> Build.VERSION_CODES.S

        else -> Constants.MIN_API
    }

    fun getMaxApi(id: ActionId): Int = when (id) {
        ActionId.SHOW_KEYBOARD_PICKER -> Build.VERSION_CODES.P

        // The global action still fails even though the API exists in SDK 34.
        ActionId.COLLAPSE_STATUS_BAR -> Build.VERSION_CODES.TIRAMISU

        ActionId.ENABLE_BLUETOOTH,
        ActionId.DISABLE_BLUETOOTH,
        ActionId.TOGGLE_BLUETOOTH,
        -> Build.VERSION_CODES.S_V2

        // See https://issuetracker.google.com/issues/225186417. The global action
        // is not marked as deprecated even though it doesn't work.
        ActionId.TOGGLE_SPLIT_SCREEN -> Build.VERSION_CODES.S

        else -> Constants.MAX_API
    }

    fun getRequiredSystemFeatures(id: ActionId): List<String> = when (id) {
        ActionId.END_PHONE_CALL,
        ActionId.ANSWER_PHONE_CALL,
        ActionId.PHONE_CALL,
        -> listOf(PackageManager.FEATURE_TELEPHONY)

        ActionId.SECURE_LOCK_DEVICE,
        -> listOf(PackageManager.FEATURE_DEVICE_ADMIN)

        ActionId.TOGGLE_WIFI,
        ActionId.ENABLE_WIFI,
        ActionId.DISABLE_WIFI,
        -> listOf(PackageManager.FEATURE_WIFI)

        ActionId.TOGGLE_NFC,
        ActionId.ENABLE_NFC,
        ActionId.DISABLE_NFC,
        -> listOf(PackageManager.FEATURE_NFC)

        ActionId.TOGGLE_BLUETOOTH,
        ActionId.ENABLE_BLUETOOTH,
        ActionId.DISABLE_BLUETOOTH,
        -> listOf(PackageManager.FEATURE_BLUETOOTH)

        ActionId.TOGGLE_FLASHLIGHT,
        ActionId.ENABLE_FLASHLIGHT,
        ActionId.DISABLE_FLASHLIGHT,
        ActionId.CHANGE_FLASHLIGHT_STRENGTH,
        -> listOf(PackageManager.FEATURE_CAMERA_FLASH)

        else -> emptyList()
    }

    fun getRequiredPermissions(id: ActionId): List<Permission> {
        when (id) {
            ActionId.TOGGLE_WIFI,
            ActionId.ENABLE_WIFI,
            ActionId.DISABLE_WIFI,
            -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return listOf(Permission.ROOT)
            }

            ActionId.TOGGLE_MOBILE_DATA,
            ActionId.ENABLE_MOBILE_DATA,
            ActionId.DISABLE_MOBILE_DATA,
            -> return listOf(Permission.ROOT)

            ActionId.PLAY_PAUSE_MEDIA_PACKAGE,
            ActionId.PAUSE_MEDIA_PACKAGE,
            ActionId.PLAY_MEDIA_PACKAGE,
            ActionId.NEXT_TRACK_PACKAGE,
            ActionId.PREVIOUS_TRACK_PACKAGE,
            ActionId.FAST_FORWARD_PACKAGE,
            ActionId.REWIND_PACKAGE,
            -> return listOf(Permission.NOTIFICATION_LISTENER)

            ActionId.VOLUME_UP,
            ActionId.VOLUME_DOWN,
            ActionId.VOLUME_INCREASE_STREAM,
            ActionId.VOLUME_DECREASE_STREAM,
            ActionId.VOLUME_SHOW_DIALOG,
            ActionId.CYCLE_RINGER_MODE,
            ActionId.CYCLE_VIBRATE_RING,
            ActionId.CHANGE_RINGER_MODE,
            ActionId.VOLUME_MUTE,
            ActionId.VOLUME_UNMUTE,
            ActionId.VOLUME_TOGGLE_MUTE,
            ActionId.TOGGLE_DND_MODE,
            ActionId.DISABLE_DND_MODE,
            ActionId.ENABLE_DND_MODE,
            -> return listOf(Permission.ACCESS_NOTIFICATION_POLICY)

            ActionId.TOGGLE_AUTO_ROTATE,
            ActionId.ENABLE_AUTO_ROTATE,
            ActionId.DISABLE_AUTO_ROTATE,
            ActionId.PORTRAIT_MODE,
            ActionId.LANDSCAPE_MODE,
            ActionId.SWITCH_ORIENTATION,
            ActionId.CYCLE_ROTATIONS,
            -> return listOf(Permission.WRITE_SETTINGS)

            ActionId.TOGGLE_AUTO_BRIGHTNESS,
            ActionId.ENABLE_AUTO_BRIGHTNESS,
            ActionId.DISABLE_AUTO_BRIGHTNESS,
            ActionId.INCREASE_BRIGHTNESS,
            ActionId.DECREASE_BRIGHTNESS,
            -> return listOf(Permission.WRITE_SETTINGS)

            ActionId.TOGGLE_FLASHLIGHT,
            ActionId.ENABLE_FLASHLIGHT,
            ActionId.DISABLE_FLASHLIGHT,
            ActionId.CHANGE_FLASHLIGHT_STRENGTH,
            -> return listOf(Permission.CAMERA)

            ActionId.ENABLE_NFC,
            ActionId.DISABLE_NFC,
            ActionId.TOGGLE_NFC,
            -> return listOf(Permission.ROOT)

            ActionId.SHOW_KEYBOARD_PICKER ->
                if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O_MR1..Build.VERSION_CODES.P) {
                    return listOf(Permission.ROOT)
                }

            ActionId.SWITCH_KEYBOARD -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                return listOf(Permission.WRITE_SECURE_SETTINGS)
            }

            ActionId.TOGGLE_AIRPLANE_MODE,
            ActionId.ENABLE_AIRPLANE_MODE,
            ActionId.DISABLE_AIRPLANE_MODE,
            -> Permission.ROOT

            ActionId.SCREENSHOT -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return listOf(Permission.ROOT)
            }

            ActionId.LOCK_DEVICE -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return listOf(Permission.ROOT)
            }

            ActionId.SECURE_LOCK_DEVICE -> return listOf(Permission.DEVICE_ADMIN)
            ActionId.POWER_ON_OFF_DEVICE -> return listOf(Permission.ROOT)

            ActionId.DISMISS_ALL_NOTIFICATIONS,
            ActionId.DISMISS_MOST_RECENT_NOTIFICATION,
            ->
                return listOf(Permission.NOTIFICATION_LISTENER)

            ActionId.ANSWER_PHONE_CALL,
            ActionId.END_PHONE_CALL,
            -> return listOf(Permission.ANSWER_PHONE_CALL)

            ActionId.PHONE_CALL -> return listOf(Permission.CALL_PHONE)

            ActionId.ENABLE_BLUETOOTH, ActionId.DISABLE_BLUETOOTH, ActionId.TOGGLE_BLUETOOTH ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    return listOf(Permission.FIND_NEARBY_DEVICES)
                }

            else -> Unit
        }

        return emptyList()
    }

    fun getComposeIcon(id: ActionId): ImageVector = when (id) {
        ActionId.TOGGLE_WIFI -> Icons.Rounded.Wifi
        ActionId.ENABLE_WIFI -> Icons.Rounded.Wifi
        ActionId.DISABLE_WIFI -> Icons.Rounded.WifiOff
        ActionId.TOGGLE_BLUETOOTH -> Icons.Rounded.Bluetooth
        ActionId.ENABLE_BLUETOOTH -> Icons.Rounded.Bluetooth
        ActionId.DISABLE_BLUETOOTH -> Icons.Rounded.BluetoothDisabled
        ActionId.TOGGLE_MOBILE_DATA -> Icons.Outlined.SignalCellular4Bar
        ActionId.ENABLE_MOBILE_DATA -> Icons.Outlined.SignalCellular4Bar
        ActionId.DISABLE_MOBILE_DATA -> Icons.Outlined.SignalCellularOff
        ActionId.TOGGLE_AUTO_BRIGHTNESS -> Icons.Outlined.BrightnessAuto
        ActionId.DISABLE_AUTO_BRIGHTNESS -> Icons.Outlined.BrightnessAuto
        ActionId.ENABLE_AUTO_BRIGHTNESS -> Icons.Outlined.BrightnessAuto
        ActionId.INCREASE_BRIGHTNESS -> Icons.Outlined.BrightnessHigh
        ActionId.DECREASE_BRIGHTNESS -> Icons.Outlined.BrightnessLow
        ActionId.TOGGLE_AUTO_ROTATE -> Icons.Outlined.ScreenRotation
        ActionId.ENABLE_AUTO_ROTATE -> Icons.Outlined.ScreenRotation
        ActionId.DISABLE_AUTO_ROTATE -> Icons.Outlined.ScreenLockRotation
        ActionId.PORTRAIT_MODE -> Icons.Outlined.StayCurrentPortrait
        ActionId.LANDSCAPE_MODE -> Icons.Outlined.StayCurrentLandscape
        ActionId.SWITCH_ORIENTATION -> Icons.Outlined.ScreenRotation
        ActionId.CYCLE_ROTATIONS -> Icons.Outlined.ScreenRotation
        ActionId.VOLUME_UP -> Icons.AutoMirrored.Outlined.VolumeUp
        ActionId.VOLUME_DOWN -> Icons.AutoMirrored.Outlined.VolumeDown
        ActionId.VOLUME_SHOW_DIALOG -> KeyMapperIcons.InstantMix
        ActionId.VOLUME_DECREASE_STREAM -> Icons.AutoMirrored.Outlined.VolumeDown
        ActionId.VOLUME_INCREASE_STREAM -> Icons.AutoMirrored.Outlined.VolumeUp
        ActionId.CYCLE_RINGER_MODE -> Icons.Outlined.PhonelinkRing
        ActionId.CHANGE_RINGER_MODE -> Icons.Outlined.PhonelinkRing
        ActionId.CYCLE_VIBRATE_RING -> Icons.Outlined.PhonelinkRing
        ActionId.TOGGLE_DND_MODE -> Icons.Outlined.DoNotDisturb
        ActionId.ENABLE_DND_MODE -> Icons.Outlined.DoNotDisturb
        ActionId.DISABLE_DND_MODE -> Icons.Outlined.DoNotDisturbOff
        ActionId.VOLUME_UNMUTE -> Icons.AutoMirrored.Outlined.VolumeUp
        ActionId.VOLUME_MUTE -> Icons.AutoMirrored.Outlined.VolumeMute
        ActionId.VOLUME_TOGGLE_MUTE -> Icons.AutoMirrored.Outlined.VolumeMute
        ActionId.EXPAND_NOTIFICATION_DRAWER -> KeyMapperIcons.TopPanelOpen
        ActionId.TOGGLE_NOTIFICATION_DRAWER -> KeyMapperIcons.TopPanelClose
        ActionId.EXPAND_QUICK_SETTINGS -> KeyMapperIcons.TopPanelOpen
        ActionId.TOGGLE_QUICK_SETTINGS -> KeyMapperIcons.TopPanelOpen
        ActionId.COLLAPSE_STATUS_BAR -> KeyMapperIcons.TopPanelClose
        ActionId.PAUSE_MEDIA -> Icons.Outlined.Pause
        ActionId.PAUSE_MEDIA_PACKAGE -> Icons.Outlined.Pause
        ActionId.PLAY_MEDIA -> Icons.Outlined.PlayArrow
        ActionId.PLAY_MEDIA_PACKAGE -> Icons.Outlined.PlayArrow
        ActionId.PLAY_PAUSE_MEDIA -> Icons.Outlined.NotStarted
        ActionId.PLAY_PAUSE_MEDIA_PACKAGE -> Icons.Outlined.NotStarted
        ActionId.NEXT_TRACK -> Icons.Outlined.SkipNext
        ActionId.NEXT_TRACK_PACKAGE -> Icons.Outlined.SkipNext
        ActionId.PREVIOUS_TRACK -> Icons.Outlined.SkipPrevious
        ActionId.PREVIOUS_TRACK_PACKAGE -> Icons.Outlined.SkipPrevious
        ActionId.FAST_FORWARD -> Icons.Outlined.FastForward
        ActionId.FAST_FORWARD_PACKAGE -> Icons.Outlined.FastForward
        ActionId.REWIND -> Icons.Outlined.FastRewind
        ActionId.REWIND_PACKAGE -> Icons.Outlined.FastRewind
        ActionId.STOP_MEDIA -> Icons.Outlined.StopCircle
        ActionId.STOP_MEDIA_PACKAGE -> Icons.Outlined.StopCircle
        ActionId.STEP_FORWARD -> Icons.Outlined.Forward30
        ActionId.STEP_FORWARD_PACKAGE -> Icons.Outlined.Forward30
        ActionId.STEP_BACKWARD -> Icons.Outlined.Replay30
        ActionId.STEP_BACKWARD_PACKAGE -> Icons.Outlined.Replay30
        ActionId.GO_BACK -> Icons.AutoMirrored.Outlined.ArrowBack
        ActionId.GO_HOME -> Icons.Outlined.Home
        ActionId.OPEN_RECENTS -> Icons.Outlined.ViewArray
        ActionId.TOGGLE_SPLIT_SCREEN -> Icons.Outlined.Splitscreen
        ActionId.GO_LAST_APP -> Icons.AutoMirrored.Outlined.Undo
        ActionId.OPEN_MENU -> Icons.Outlined.MoreVert
        ActionId.TOGGLE_FLASHLIGHT -> Icons.Outlined.FlashlightOn
        ActionId.ENABLE_FLASHLIGHT -> Icons.Outlined.FlashlightOn
        ActionId.DISABLE_FLASHLIGHT -> Icons.Outlined.FlashlightOff
        ActionId.CHANGE_FLASHLIGHT_STRENGTH -> Icons.Outlined.FlashlightOn
        ActionId.ENABLE_NFC -> Icons.Outlined.Nfc
        ActionId.DISABLE_NFC -> KeyMapperIcons.NfcOff
        ActionId.TOGGLE_NFC -> Icons.Outlined.Nfc
        ActionId.MOVE_CURSOR_TO_END -> KeyMapperIcons.TextSelectEnd
        ActionId.TOGGLE_KEYBOARD -> Icons.Outlined.Keyboard
        ActionId.SHOW_KEYBOARD -> Icons.Outlined.Keyboard
        ActionId.HIDE_KEYBOARD -> Icons.Outlined.KeyboardHide
        ActionId.SHOW_KEYBOARD_PICKER -> Icons.Outlined.Keyboard
        ActionId.TEXT_CUT -> Icons.Rounded.ContentCut
        ActionId.TEXT_COPY -> Icons.Rounded.ContentCopy
        ActionId.TEXT_PASTE -> Icons.Rounded.ContentPaste
        ActionId.SELECT_WORD_AT_CURSOR -> KeyMapperIcons.MatchWord
        ActionId.SWITCH_KEYBOARD -> Icons.Outlined.Keyboard
        ActionId.TOGGLE_AIRPLANE_MODE -> Icons.Outlined.AirplanemodeActive
        ActionId.ENABLE_AIRPLANE_MODE -> Icons.Outlined.AirplanemodeActive
        ActionId.DISABLE_AIRPLANE_MODE -> Icons.Outlined.AirplanemodeInactive
        ActionId.SCREENSHOT -> Icons.Outlined.Fullscreen
        ActionId.OPEN_VOICE_ASSISTANT -> Icons.Outlined.Assistant
        ActionId.OPEN_DEVICE_ASSISTANT -> Icons.Outlined.Assistant
        ActionId.OPEN_CAMERA -> Icons.Outlined.CameraAlt
        ActionId.LOCK_DEVICE -> Icons.Outlined.Lock
        ActionId.POWER_ON_OFF_DEVICE -> Icons.Outlined.PowerSettingsNew
        ActionId.SECURE_LOCK_DEVICE -> Icons.Outlined.Lock
        ActionId.CONSUME_KEY_EVENT -> Icons.Outlined.Cancel
        ActionId.OPEN_SETTINGS -> Icons.Outlined.Settings
        ActionId.SHOW_POWER_MENU -> Icons.Outlined.PowerSettingsNew
        ActionId.APP -> Icons.Rounded.Android
        ActionId.APP_SHORTCUT -> Icons.AutoMirrored.Outlined.OpenInNew
        ActionId.KEY_CODE -> Icons.Rounded.Abc
        ActionId.KEY_EVENT -> Icons.Rounded.Abc
        ActionId.TAP_SCREEN -> Icons.Outlined.TouchApp
        ActionId.SWIPE_SCREEN -> Icons.Outlined.Swipe
        ActionId.PINCH_SCREEN -> Icons.Outlined.Pinch
        ActionId.TEXT -> Icons.AutoMirrored.Outlined.ShortText
        ActionId.URL -> Icons.Outlined.Link
        ActionId.INTENT -> Icons.Outlined.DataObject
        ActionId.PHONE_CALL -> Icons.Outlined.Call
        ActionId.SOUND -> Icons.AutoMirrored.Outlined.VolumeUp
        ActionId.DISMISS_MOST_RECENT_NOTIFICATION -> Icons.Outlined.ClearAll
        ActionId.DISMISS_ALL_NOTIFICATIONS -> Icons.Outlined.ClearAll
        ActionId.ANSWER_PHONE_CALL -> Icons.Outlined.Call
        ActionId.END_PHONE_CALL -> Icons.Outlined.CallEnd
        ActionId.DEVICE_CONTROLS -> KeyMapperIcons.HomeIotDevice
        ActionId.HTTP_REQUEST -> Icons.Outlined.Http
        ActionId.INTERACT_UI_ELEMENT -> KeyMapperIcons.JumpToElement
    }
}

fun ActionData.canBeHeldDown(): Boolean = when (this) {
    is ActionData.InputKeyEvent -> !useShell
    is ActionData.TapScreen -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    else -> false
}

fun ActionData.canUseImeToPerform(): Boolean = when (this) {
    is ActionData.InputKeyEvent -> !useShell
    is ActionData.Text -> true
    is ActionData.MoveCursorToEnd -> true
    else -> false
}

fun ActionData.canUseShizukuToPerform(): Boolean = when (this) {
    is ActionData.InputKeyEvent -> true
    is ActionData.MoveCursorToEnd -> true
    else -> false
}

fun ActionData.isEditable(): Boolean = when (this) {
    is ActionData.App,
    is ActionData.AppShortcut,
    is ActionData.InputKeyEvent,
    is ActionData.Intent,
    is ActionData.Sound,
    is ActionData.SwitchKeyboard,
    is ActionData.ControlMediaForApp,
    is ActionData.Volume.Up,
    is ActionData.Volume.Down,
    is ActionData.Volume.Mute,
    is ActionData.Volume.UnMute,
    is ActionData.Volume.ToggleMute,
    is ActionData.Volume.Stream.Increase,
    is ActionData.Volume.Stream.Decrease,
    is ActionData.Volume.SetRingerMode,
    is ActionData.DoNotDisturb.Enable,
    is ActionData.DoNotDisturb.Toggle,
    is ActionData.Rotation.CycleRotations,
    is ActionData.Flashlight.Toggle,
    is ActionData.Flashlight.Enable,
    is ActionData.Flashlight.ChangeStrength,
    is ActionData.TapScreen,
    is ActionData.SwipeScreen,
    is ActionData.PinchScreen,
    is ActionData.Text,
    is ActionData.Url,
    is ActionData.PhoneCall,
    is ActionData.HttpRequest,
    is ActionData.InteractUiElement,
    -> true

    else -> false
}
