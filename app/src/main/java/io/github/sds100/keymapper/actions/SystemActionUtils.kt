package io.github.sds100.keymapper.actions

import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.permissions.Permission

/**
 * Created by sds100 on 01/08/2018.
 */

object SystemActionUtils {

    @StringRes
    fun getCategoryLabel(category: SystemActionCategory): Int =
        when (category) {
            SystemActionCategory.WIFI -> R.string.system_action_cat_wifi
            SystemActionCategory.BLUETOOTH -> R.string.system_action_cat_bluetooth
            SystemActionCategory.MOBILE_DATA -> R.string.system_action_cat_mobile_data
            SystemActionCategory.NAVIGATION -> R.string.system_action_cat_navigation
            SystemActionCategory.SCREEN_ROTATION -> R.string.system_action_cat_screen_rotation
            SystemActionCategory.VOLUME -> R.string.system_action_cat_volume
            SystemActionCategory.BRIGHTNESS -> R.string.system_action_cat_brightness
            SystemActionCategory.STATUS_BAR -> R.string.system_action_cat_status_bar
            SystemActionCategory.MEDIA -> R.string.system_action_cat_media
            SystemActionCategory.FLASHLIGHT -> R.string.system_action_cat_flashlight
            SystemActionCategory.KEYBOARD -> R.string.system_action_cat_keyboard
            SystemActionCategory.NFC -> R.string.system_action_cat_nfc
            SystemActionCategory.AIRPLANE_MODE -> R.string.system_action_cat_airplane_mode
            SystemActionCategory.OTHER -> R.string.system_action_cat_other
        }

    fun getCategory(id: SystemActionId): SystemActionCategory =
        when (id) {
            SystemActionId.TOGGLE_WIFI -> SystemActionCategory.WIFI
            SystemActionId.ENABLE_WIFI -> SystemActionCategory.WIFI
            SystemActionId.DISABLE_WIFI -> SystemActionCategory.WIFI

            SystemActionId.TOGGLE_BLUETOOTH -> SystemActionCategory.BLUETOOTH
            SystemActionId.ENABLE_BLUETOOTH -> SystemActionCategory.BLUETOOTH
            SystemActionId.DISABLE_BLUETOOTH -> SystemActionCategory.BLUETOOTH

            SystemActionId.TOGGLE_MOBILE_DATA -> SystemActionCategory.MOBILE_DATA
            SystemActionId.ENABLE_MOBILE_DATA -> SystemActionCategory.MOBILE_DATA
            SystemActionId.DISABLE_MOBILE_DATA -> SystemActionCategory.MOBILE_DATA

            SystemActionId.TOGGLE_AUTO_BRIGHTNESS -> SystemActionCategory.BRIGHTNESS
            SystemActionId.DISABLE_AUTO_BRIGHTNESS -> SystemActionCategory.BRIGHTNESS
            SystemActionId.ENABLE_AUTO_BRIGHTNESS -> SystemActionCategory.BRIGHTNESS
            SystemActionId.INCREASE_BRIGHTNESS -> SystemActionCategory.BRIGHTNESS
            SystemActionId.DECREASE_BRIGHTNESS -> SystemActionCategory.BRIGHTNESS

            SystemActionId.TOGGLE_AUTO_ROTATE -> SystemActionCategory.SCREEN_ROTATION
            SystemActionId.ENABLE_AUTO_ROTATE -> SystemActionCategory.SCREEN_ROTATION
            SystemActionId.DISABLE_AUTO_ROTATE -> SystemActionCategory.SCREEN_ROTATION
            SystemActionId.PORTRAIT_MODE -> SystemActionCategory.SCREEN_ROTATION
            SystemActionId.LANDSCAPE_MODE -> SystemActionCategory.SCREEN_ROTATION
            SystemActionId.SWITCH_ORIENTATION -> SystemActionCategory.SCREEN_ROTATION
            SystemActionId.CYCLE_ROTATIONS -> SystemActionCategory.SCREEN_ROTATION

            SystemActionId.VOLUME_UP -> SystemActionCategory.VOLUME
            SystemActionId.VOLUME_DOWN -> SystemActionCategory.VOLUME
            SystemActionId.VOLUME_SHOW_DIALOG -> SystemActionCategory.VOLUME
            SystemActionId.VOLUME_DECREASE_STREAM -> SystemActionCategory.VOLUME
            SystemActionId.VOLUME_INCREASE_STREAM -> SystemActionCategory.VOLUME
            SystemActionId.CYCLE_RINGER_MODE -> SystemActionCategory.VOLUME
            SystemActionId.CHANGE_RINGER_MODE -> SystemActionCategory.VOLUME
            SystemActionId.CYCLE_VIBRATE_RING -> SystemActionCategory.VOLUME
            SystemActionId.TOGGLE_DND_MODE -> SystemActionCategory.VOLUME
            SystemActionId.ENABLE_DND_MODE -> SystemActionCategory.VOLUME
            SystemActionId.DISABLE_DND_MODE -> SystemActionCategory.VOLUME
            SystemActionId.VOLUME_UNMUTE -> SystemActionCategory.VOLUME
            SystemActionId.VOLUME_MUTE -> SystemActionCategory.VOLUME
            SystemActionId.VOLUME_TOGGLE_MUTE -> SystemActionCategory.VOLUME

            SystemActionId.EXPAND_NOTIFICATION_DRAWER -> SystemActionCategory.STATUS_BAR
            SystemActionId.TOGGLE_NOTIFICATION_DRAWER -> SystemActionCategory.STATUS_BAR
            SystemActionId.EXPAND_QUICK_SETTINGS -> SystemActionCategory.STATUS_BAR
            SystemActionId.TOGGLE_QUICK_SETTINGS -> SystemActionCategory.STATUS_BAR
            SystemActionId.COLLAPSE_STATUS_BAR -> SystemActionCategory.STATUS_BAR

            SystemActionId.PAUSE_MEDIA -> SystemActionCategory.MEDIA
            SystemActionId.PAUSE_MEDIA_PACKAGE -> SystemActionCategory.MEDIA
            SystemActionId.PLAY_MEDIA -> SystemActionCategory.MEDIA
            SystemActionId.PLAY_MEDIA_PACKAGE -> SystemActionCategory.MEDIA
            SystemActionId.PLAY_PAUSE_MEDIA -> SystemActionCategory.MEDIA
            SystemActionId.PLAY_PAUSE_MEDIA_PACKAGE -> SystemActionCategory.MEDIA
            SystemActionId.NEXT_TRACK -> SystemActionCategory.MEDIA
            SystemActionId.NEXT_TRACK_PACKAGE -> SystemActionCategory.MEDIA
            SystemActionId.PREVIOUS_TRACK -> SystemActionCategory.MEDIA
            SystemActionId.PREVIOUS_TRACK_PACKAGE -> SystemActionCategory.MEDIA
            SystemActionId.FAST_FORWARD -> SystemActionCategory.MEDIA
            SystemActionId.FAST_FORWARD_PACKAGE -> SystemActionCategory.MEDIA
            SystemActionId.REWIND -> SystemActionCategory.MEDIA
            SystemActionId.REWIND_PACKAGE -> SystemActionCategory.MEDIA

            SystemActionId.GO_BACK -> SystemActionCategory.NAVIGATION
            SystemActionId.GO_HOME -> SystemActionCategory.NAVIGATION
            SystemActionId.OPEN_RECENTS -> SystemActionCategory.NAVIGATION
            SystemActionId.TOGGLE_SPLIT_SCREEN -> SystemActionCategory.NAVIGATION
            SystemActionId.GO_LAST_APP -> SystemActionCategory.NAVIGATION
            SystemActionId.OPEN_MENU -> SystemActionCategory.NAVIGATION

            SystemActionId.TOGGLE_FLASHLIGHT -> SystemActionCategory.FLASHLIGHT
            SystemActionId.ENABLE_FLASHLIGHT -> SystemActionCategory.FLASHLIGHT
            SystemActionId.DISABLE_FLASHLIGHT -> SystemActionCategory.FLASHLIGHT

            SystemActionId.ENABLE_NFC -> SystemActionCategory.NFC
            SystemActionId.DISABLE_NFC -> SystemActionCategory.NFC
            SystemActionId.TOGGLE_NFC -> SystemActionCategory.NFC

            SystemActionId.MOVE_CURSOR_TO_END -> SystemActionCategory.KEYBOARD
            SystemActionId.TOGGLE_KEYBOARD -> SystemActionCategory.KEYBOARD
            SystemActionId.SHOW_KEYBOARD -> SystemActionCategory.KEYBOARD
            SystemActionId.HIDE_KEYBOARD -> SystemActionCategory.KEYBOARD
            SystemActionId.SHOW_KEYBOARD_PICKER -> SystemActionCategory.KEYBOARD
            SystemActionId.TEXT_CUT -> SystemActionCategory.KEYBOARD
            SystemActionId.TEXT_COPY -> SystemActionCategory.KEYBOARD
            SystemActionId.TEXT_PASTE -> SystemActionCategory.KEYBOARD
            SystemActionId.SELECT_WORD_AT_CURSOR -> SystemActionCategory.KEYBOARD
            SystemActionId.SWITCH_KEYBOARD -> SystemActionCategory.KEYBOARD

            SystemActionId.TOGGLE_AIRPLANE_MODE -> SystemActionCategory.AIRPLANE_MODE
            SystemActionId.ENABLE_AIRPLANE_MODE -> SystemActionCategory.AIRPLANE_MODE
            SystemActionId.DISABLE_AIRPLANE_MODE -> SystemActionCategory.AIRPLANE_MODE

            SystemActionId.SCREENSHOT -> SystemActionCategory.OTHER
            SystemActionId.OPEN_VOICE_ASSISTANT -> SystemActionCategory.OTHER
            SystemActionId.OPEN_DEVICE_ASSISTANT -> SystemActionCategory.OTHER
            SystemActionId.OPEN_CAMERA -> SystemActionCategory.OTHER
            SystemActionId.LOCK_DEVICE -> SystemActionCategory.OTHER
            SystemActionId.POWER_ON_OFF_DEVICE -> SystemActionCategory.OTHER
            SystemActionId.SECURE_LOCK_DEVICE -> SystemActionCategory.OTHER
            SystemActionId.CONSUME_KEY_EVENT -> SystemActionCategory.OTHER
            SystemActionId.OPEN_SETTINGS -> SystemActionCategory.OTHER
            SystemActionId.SHOW_POWER_MENU -> SystemActionCategory.OTHER
        }

    @StringRes
    fun getTitle(id: SystemActionId): Int =
        when (id) {
            SystemActionId.TOGGLE_WIFI -> R.string.action_toggle_wifi
            SystemActionId.ENABLE_WIFI -> R.string.action_enable_wifi
            SystemActionId.DISABLE_WIFI -> R.string.action_disable_wifi
            SystemActionId.TOGGLE_BLUETOOTH -> R.string.action_toggle_bluetooth
            SystemActionId.ENABLE_BLUETOOTH -> R.string.action_enable_bluetooth
            SystemActionId.DISABLE_BLUETOOTH -> R.string.action_disable_bluetooth
            SystemActionId.TOGGLE_MOBILE_DATA -> R.string.action_toggle_mobile_data
            SystemActionId.ENABLE_MOBILE_DATA -> R.string.action_enable_mobile_data
            SystemActionId.DISABLE_MOBILE_DATA -> R.string.action_disable_mobile_data
            SystemActionId.TOGGLE_AUTO_BRIGHTNESS -> R.string.action_toggle_auto_brightness
            SystemActionId.DISABLE_AUTO_BRIGHTNESS -> R.string.action_disable_auto_brightness
            SystemActionId.ENABLE_AUTO_BRIGHTNESS -> R.string.action_enable_auto_brightness
            SystemActionId.INCREASE_BRIGHTNESS -> R.string.action_increase_brightness
            SystemActionId.DECREASE_BRIGHTNESS -> R.string.action_decrease_brightness
            SystemActionId.TOGGLE_AUTO_ROTATE -> R.string.action_toggle_auto_rotate
            SystemActionId.ENABLE_AUTO_ROTATE -> R.string.action_enable_auto_rotate
            SystemActionId.DISABLE_AUTO_ROTATE -> R.string.action_disable_auto_rotate
            SystemActionId.PORTRAIT_MODE -> R.string.action_portrait_mode
            SystemActionId.LANDSCAPE_MODE -> R.string.action_landscape_mode
            SystemActionId.SWITCH_ORIENTATION -> R.string.action_switch_orientation
            SystemActionId.CYCLE_ROTATIONS -> R.string.action_cycle_rotations
            SystemActionId.VOLUME_UP -> R.string.action_volume_up
            SystemActionId.VOLUME_DOWN -> R.string.action_volume_down
            SystemActionId.VOLUME_SHOW_DIALOG -> R.string.action_volume_show_dialog
            SystemActionId.VOLUME_DECREASE_STREAM -> R.string.action_decrease_stream
            SystemActionId.VOLUME_INCREASE_STREAM -> R.string.action_increase_stream
            SystemActionId.CYCLE_RINGER_MODE -> R.string.action_cycle_ringer_mode
            SystemActionId.CHANGE_RINGER_MODE -> R.string.action_change_ringer_mode
            SystemActionId.CYCLE_VIBRATE_RING -> R.string.action_cycle_vibrate_ring
            SystemActionId.TOGGLE_DND_MODE -> R.string.action_toggle_dnd_mode
            SystemActionId.ENABLE_DND_MODE -> R.string.action_enable_dnd_mode
            SystemActionId.DISABLE_DND_MODE -> R.string.action_disable_dnd_mode
            SystemActionId.VOLUME_UNMUTE -> R.string.action_volume_unmute
            SystemActionId.VOLUME_MUTE -> R.string.action_volume_mute
            SystemActionId.VOLUME_TOGGLE_MUTE -> R.string.action_toggle_mute
            SystemActionId.EXPAND_NOTIFICATION_DRAWER -> R.string.action_expand_notification_drawer
            SystemActionId.TOGGLE_NOTIFICATION_DRAWER -> R.string.action_toggle_notification_drawer
            SystemActionId.EXPAND_QUICK_SETTINGS -> R.string.action_expand_quick_settings
            SystemActionId.TOGGLE_QUICK_SETTINGS -> R.string.action_toggle_quick_settings
            SystemActionId.COLLAPSE_STATUS_BAR -> R.string.action_collapse_status_bar
            SystemActionId.PAUSE_MEDIA -> R.string.action_pause_media
            SystemActionId.PAUSE_MEDIA_PACKAGE -> R.string.action_pause_media_package
            SystemActionId.PLAY_MEDIA -> R.string.action_play_media
            SystemActionId.PLAY_MEDIA_PACKAGE -> R.string.action_play_media_package
            SystemActionId.PLAY_PAUSE_MEDIA -> R.string.action_play_pause_media
            SystemActionId.PLAY_PAUSE_MEDIA_PACKAGE -> R.string.action_play_pause_media_package
            SystemActionId.NEXT_TRACK -> R.string.action_next_track
            SystemActionId.NEXT_TRACK_PACKAGE -> R.string.action_next_track_package
            SystemActionId.PREVIOUS_TRACK -> R.string.action_previous_track
            SystemActionId.PREVIOUS_TRACK_PACKAGE -> R.string.action_previous_track_package
            SystemActionId.FAST_FORWARD -> R.string.action_fast_forward
            SystemActionId.FAST_FORWARD_PACKAGE -> R.string.action_fast_forward_package
            SystemActionId.REWIND -> R.string.action_rewind
            SystemActionId.REWIND_PACKAGE -> R.string.action_rewind_package
            SystemActionId.GO_BACK -> R.string.action_go_back
            SystemActionId.GO_HOME -> R.string.action_go_home
            SystemActionId.OPEN_RECENTS -> R.string.action_open_recents
            SystemActionId.TOGGLE_SPLIT_SCREEN -> R.string.action_toggle_split_screen
            SystemActionId.GO_LAST_APP -> R.string.action_go_last_app
            SystemActionId.OPEN_MENU -> R.string.action_open_menu
            SystemActionId.TOGGLE_FLASHLIGHT -> R.string.action_toggle_flashlight
            SystemActionId.ENABLE_FLASHLIGHT -> R.string.action_enable_flashlight
            SystemActionId.DISABLE_FLASHLIGHT -> R.string.action_disable_flashlight
            SystemActionId.ENABLE_NFC -> R.string.action_nfc_enable
            SystemActionId.DISABLE_NFC -> R.string.action_nfc_disable
            SystemActionId.TOGGLE_NFC -> R.string.action_nfc_toggle
            SystemActionId.MOVE_CURSOR_TO_END -> R.string.action_move_to_end_of_text
            SystemActionId.TOGGLE_KEYBOARD -> R.string.action_toggle_keyboard
            SystemActionId.SHOW_KEYBOARD -> R.string.action_show_keyboard
            SystemActionId.HIDE_KEYBOARD -> R.string.action_hide_keyboard
            SystemActionId.SHOW_KEYBOARD_PICKER -> R.string.action_show_keyboard_picker
            SystemActionId.TEXT_CUT -> R.string.action_text_cut
            SystemActionId.TEXT_COPY -> R.string.action_text_copy
            SystemActionId.TEXT_PASTE -> R.string.action_text_paste
            SystemActionId.SELECT_WORD_AT_CURSOR -> R.string.action_select_word_at_cursor
            SystemActionId.SWITCH_KEYBOARD -> R.string.action_switch_keyboard
            SystemActionId.TOGGLE_AIRPLANE_MODE -> R.string.action_toggle_airplane_mode
            SystemActionId.ENABLE_AIRPLANE_MODE -> R.string.action_enable_airplane_mode
            SystemActionId.DISABLE_AIRPLANE_MODE -> R.string.action_disable_airplane_mode
            SystemActionId.SCREENSHOT -> R.string.action_screenshot
            SystemActionId.OPEN_VOICE_ASSISTANT -> R.string.action_open_assistant
            SystemActionId.OPEN_DEVICE_ASSISTANT -> R.string.action_open_device_assistant
            SystemActionId.OPEN_CAMERA -> R.string.action_open_camera
            SystemActionId.LOCK_DEVICE -> R.string.action_lock_device
            SystemActionId.POWER_ON_OFF_DEVICE -> R.string.action_power_on_off_device
            SystemActionId.SECURE_LOCK_DEVICE -> R.string.action_secure_lock_device
            SystemActionId.CONSUME_KEY_EVENT -> R.string.action_consume_keyevent
            SystemActionId.OPEN_SETTINGS -> R.string.action_open_settings
            SystemActionId.SHOW_POWER_MENU -> R.string.action_show_power_menu
        }

    @DrawableRes
    fun getIcon(id: SystemActionId): Int? =
        when (id) {
            SystemActionId.TOGGLE_WIFI -> R.drawable.ic_outline_wifi_24
            SystemActionId.ENABLE_WIFI -> R.drawable.ic_outline_wifi_24
            SystemActionId.DISABLE_WIFI -> R.drawable.ic_outline_wifi_off_24
            SystemActionId.TOGGLE_BLUETOOTH -> R.drawable.ic_outline_bluetooth_24
            SystemActionId.ENABLE_BLUETOOTH -> R.drawable.ic_outline_bluetooth_24
            SystemActionId.DISABLE_BLUETOOTH -> R.drawable.ic_outline_bluetooth_disabled_24
            SystemActionId.TOGGLE_MOBILE_DATA -> R.drawable.ic_outline_signal_cellular_4_bar_24
            SystemActionId.ENABLE_MOBILE_DATA -> R.drawable.ic_outline_signal_cellular_4_bar_24
            SystemActionId.DISABLE_MOBILE_DATA -> R.drawable.ic_outline_signal_cellular_off_24
            SystemActionId.TOGGLE_AUTO_BRIGHTNESS -> R.drawable.ic_outline_brightness_auto_24
            SystemActionId.DISABLE_AUTO_BRIGHTNESS -> R.drawable.ic_disable_brightness_auto_24dp
            SystemActionId.ENABLE_AUTO_BRIGHTNESS -> R.drawable.ic_outline_brightness_auto_24
            SystemActionId.INCREASE_BRIGHTNESS -> R.drawable.ic_outline_brightness_high_24
            SystemActionId.DECREASE_BRIGHTNESS -> R.drawable.ic_outline_brightness_low_24
            SystemActionId.TOGGLE_AUTO_ROTATE -> R.drawable.ic_outline_screen_rotation_24
            SystemActionId.ENABLE_AUTO_ROTATE -> R.drawable.ic_outline_screen_rotation_24
            SystemActionId.DISABLE_AUTO_ROTATE -> R.drawable.ic_outline_screen_lock_rotation_24
            SystemActionId.PORTRAIT_MODE -> R.drawable.ic_outline_stay_current_portrait_24
            SystemActionId.LANDSCAPE_MODE -> R.drawable.ic_outline_stay_current_landscape_24
            SystemActionId.SWITCH_ORIENTATION -> R.drawable.ic_outline_screen_rotation_24
            SystemActionId.CYCLE_ROTATIONS -> R.drawable.ic_outline_screen_rotation_24
            SystemActionId.VOLUME_UP -> R.drawable.ic_outline_volume_up_24
            SystemActionId.VOLUME_DOWN -> R.drawable.ic_outline_volume_down_24
            SystemActionId.VOLUME_SHOW_DIALOG -> null
            SystemActionId.VOLUME_DECREASE_STREAM -> R.drawable.ic_outline_volume_down_24
            SystemActionId.VOLUME_INCREASE_STREAM -> R.drawable.ic_outline_volume_up_24
            SystemActionId.CYCLE_RINGER_MODE -> null
            SystemActionId.CHANGE_RINGER_MODE -> null
            SystemActionId.CYCLE_VIBRATE_RING -> null
            SystemActionId.TOGGLE_DND_MODE -> R.drawable.dnd_circle_outline
            SystemActionId.ENABLE_DND_MODE -> R.drawable.dnd_circle_outline
            SystemActionId.DISABLE_DND_MODE -> R.drawable.dnd_circle_off_outline
            SystemActionId.VOLUME_UNMUTE -> R.drawable.ic_outline_volume_up_24
            SystemActionId.VOLUME_MUTE -> R.drawable.ic_outline_volume_mute_24
            SystemActionId.VOLUME_TOGGLE_MUTE -> R.drawable.ic_outline_volume_mute_24
            SystemActionId.EXPAND_NOTIFICATION_DRAWER -> null
            SystemActionId.TOGGLE_NOTIFICATION_DRAWER -> null
            SystemActionId.EXPAND_QUICK_SETTINGS -> null
            SystemActionId.TOGGLE_QUICK_SETTINGS -> null
            SystemActionId.COLLAPSE_STATUS_BAR -> null
            SystemActionId.PAUSE_MEDIA -> R.drawable.ic_outline_pause_24
            SystemActionId.PAUSE_MEDIA_PACKAGE -> R.drawable.ic_outline_pause_24
            SystemActionId.PLAY_MEDIA -> R.drawable.ic_outline_play_arrow_24
            SystemActionId.PLAY_MEDIA_PACKAGE -> R.drawable.ic_outline_play_arrow_24
            SystemActionId.PLAY_PAUSE_MEDIA -> R.drawable.ic_play_pause_24dp
            SystemActionId.PLAY_PAUSE_MEDIA_PACKAGE -> R.drawable.ic_play_pause_24dp
            SystemActionId.NEXT_TRACK -> R.drawable.ic_outline_skip_next_24
            SystemActionId.NEXT_TRACK_PACKAGE -> R.drawable.ic_outline_skip_next_24
            SystemActionId.PREVIOUS_TRACK -> R.drawable.ic_outline_skip_previous_24
            SystemActionId.PREVIOUS_TRACK_PACKAGE -> R.drawable.ic_outline_skip_previous_24
            SystemActionId.FAST_FORWARD -> R.drawable.ic_outline_fast_forward_24
            SystemActionId.FAST_FORWARD_PACKAGE -> R.drawable.ic_outline_fast_forward_24
            SystemActionId.REWIND -> R.drawable.ic_outline_fast_rewind_24
            SystemActionId.REWIND_PACKAGE -> R.drawable.ic_outline_fast_rewind_24
            SystemActionId.GO_BACK -> R.drawable.ic_baseline_arrow_back_24
            SystemActionId.GO_HOME -> R.drawable.ic_outline_home_24
            SystemActionId.OPEN_RECENTS -> null
            SystemActionId.TOGGLE_SPLIT_SCREEN -> null
            SystemActionId.GO_LAST_APP -> null
            SystemActionId.OPEN_MENU -> R.drawable.ic_outline_more_vert_24
            SystemActionId.TOGGLE_FLASHLIGHT -> R.drawable.ic_flashlight
            SystemActionId.ENABLE_FLASHLIGHT -> R.drawable.ic_flashlight
            SystemActionId.DISABLE_FLASHLIGHT -> R.drawable.ic_flashlight_off
            SystemActionId.ENABLE_NFC -> R.drawable.ic_outline_nfc_24
            SystemActionId.DISABLE_NFC -> R.drawable.ic_nfc_off
            SystemActionId.TOGGLE_NFC -> R.drawable.ic_outline_nfc_24
            SystemActionId.MOVE_CURSOR_TO_END -> R.drawable.ic_cursor
            SystemActionId.TOGGLE_KEYBOARD -> R.drawable.ic_outline_keyboard_24
            SystemActionId.SHOW_KEYBOARD -> R.drawable.ic_outline_keyboard_24
            SystemActionId.HIDE_KEYBOARD -> R.drawable.ic_outline_keyboard_hide_24
            SystemActionId.SHOW_KEYBOARD_PICKER -> R.drawable.ic_outline_keyboard_24
            SystemActionId.TEXT_CUT -> R.drawable.ic_content_cut
            SystemActionId.TEXT_COPY -> R.drawable.ic_content_copy
            SystemActionId.TEXT_PASTE -> R.drawable.ic_content_paste
            SystemActionId.SELECT_WORD_AT_CURSOR -> null
            SystemActionId.SWITCH_KEYBOARD -> R.drawable.ic_outline_keyboard_24
            SystemActionId.TOGGLE_AIRPLANE_MODE -> R.drawable.ic_outline_airplanemode_active_24
            SystemActionId.ENABLE_AIRPLANE_MODE -> R.drawable.ic_outline_airplanemode_active_24
            SystemActionId.DISABLE_AIRPLANE_MODE -> R.drawable.ic_outline_airplanemode_inactive_24
            SystemActionId.SCREENSHOT -> R.drawable.ic_outline_fullscreen_24
            SystemActionId.OPEN_VOICE_ASSISTANT -> R.drawable.ic_outline_assistant_24
            SystemActionId.OPEN_DEVICE_ASSISTANT -> R.drawable.ic_outline_assistant_24
            SystemActionId.OPEN_CAMERA -> R.drawable.ic_outline_camera_alt_24
            SystemActionId.LOCK_DEVICE -> R.drawable.ic_outline_lock_24
            SystemActionId.POWER_ON_OFF_DEVICE -> R.drawable.ic_outline_power_settings_new_24
            SystemActionId.SECURE_LOCK_DEVICE -> R.drawable.ic_outline_lock_24
            SystemActionId.CONSUME_KEY_EVENT -> null
            SystemActionId.OPEN_SETTINGS -> R.drawable.ic_outline_settings_24
            SystemActionId.SHOW_POWER_MENU -> R.drawable.ic_outline_power_settings_new_24
        }

    fun getMinApi(id: SystemActionId): Int {
        return when (id) {
            SystemActionId.TOGGLE_SPLIT_SCREEN -> Build.VERSION_CODES.N
            SystemActionId.GO_LAST_APP -> Build.VERSION_CODES.N

            SystemActionId.VOLUME_MUTE,
            SystemActionId.VOLUME_UNMUTE,
            SystemActionId.VOLUME_TOGGLE_MUTE,
            SystemActionId.TOGGLE_DND_MODE,
            SystemActionId.ENABLE_DND_MODE,
            SystemActionId.DISABLE_DND_MODE,
            -> Build.VERSION_CODES.M

            SystemActionId.DISABLE_FLASHLIGHT,
            SystemActionId.ENABLE_FLASHLIGHT,
            SystemActionId.TOGGLE_FLASHLIGHT,
            -> Build.VERSION_CODES.M

            SystemActionId.TOGGLE_KEYBOARD,
            SystemActionId.SHOW_KEYBOARD,
            SystemActionId.HIDE_KEYBOARD,
            -> Build.VERSION_CODES.N

            SystemActionId.TEXT_CUT,
            SystemActionId.TEXT_COPY,
            SystemActionId.TEXT_PASTE,
            SystemActionId.SELECT_WORD_AT_CURSOR,
            -> Build.VERSION_CODES.JELLY_BEAN_MR2

            SystemActionId.SHOW_POWER_MENU -> Build.VERSION_CODES.LOLLIPOP

            else -> Constants.MIN_API
        }
    }

    fun getMaxApi(id: SystemActionId): Int {
        return when (id) {
            SystemActionId.SHOW_KEYBOARD_PICKER -> Build.VERSION_CODES.P

            else -> Constants.MAX_API
        }
    }

    fun getRequiredSystemFeatures(id: SystemActionId): List<String> {
        return when (id) {
            SystemActionId.SECURE_LOCK_DEVICE
            -> listOf(PackageManager.FEATURE_DEVICE_ADMIN)

            SystemActionId.TOGGLE_WIFI,
            SystemActionId.ENABLE_WIFI,
            SystemActionId.DISABLE_WIFI,
            -> listOf(PackageManager.FEATURE_WIFI)

            SystemActionId.TOGGLE_NFC,
            SystemActionId.ENABLE_NFC,
            SystemActionId.DISABLE_NFC,
            -> listOf(PackageManager.FEATURE_NFC)

            SystemActionId.TOGGLE_BLUETOOTH,
            SystemActionId.ENABLE_BLUETOOTH,
            SystemActionId.DISABLE_BLUETOOTH,
                -> listOf(PackageManager.FEATURE_BLUETOOTH)

            SystemActionId.TOGGLE_FLASHLIGHT,
            SystemActionId.ENABLE_FLASHLIGHT,
            SystemActionId.DISABLE_FLASHLIGHT,
            -> listOf(PackageManager.FEATURE_CAMERA_FLASH)

            else -> emptyList()
        }
    }

    fun getRequiredPermissions(id: SystemActionId): List<Permission> {
        when (id) {
            SystemActionId.TOGGLE_WIFI,
            SystemActionId.ENABLE_WIFI,
            SystemActionId.DISABLE_WIFI,
            -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return listOf(Permission.ROOT)
            }

            SystemActionId.TOGGLE_MOBILE_DATA,
            SystemActionId.ENABLE_MOBILE_DATA,
            SystemActionId.DISABLE_MOBILE_DATA,
            -> return listOf(Permission.ROOT)

            SystemActionId.PLAY_PAUSE_MEDIA_PACKAGE,
            SystemActionId.PAUSE_MEDIA_PACKAGE,
            SystemActionId.PLAY_MEDIA_PACKAGE,
            SystemActionId.NEXT_TRACK_PACKAGE,
            SystemActionId.PREVIOUS_TRACK_PACKAGE,
            SystemActionId.FAST_FORWARD_PACKAGE,
            SystemActionId.REWIND_PACKAGE,
            -> return listOf(Permission.NOTIFICATION_LISTENER)

            SystemActionId.VOLUME_UP,
            SystemActionId.VOLUME_DOWN,
            SystemActionId.VOLUME_INCREASE_STREAM,
            SystemActionId.VOLUME_DECREASE_STREAM,
            SystemActionId.VOLUME_SHOW_DIALOG,
            SystemActionId.CYCLE_RINGER_MODE,
            SystemActionId.CYCLE_VIBRATE_RING,
            SystemActionId.CHANGE_RINGER_MODE,
            SystemActionId.VOLUME_MUTE,
            SystemActionId.VOLUME_UNMUTE,
            SystemActionId.VOLUME_TOGGLE_MUTE,
            SystemActionId.TOGGLE_DND_MODE,
            SystemActionId.DISABLE_DND_MODE,
            SystemActionId.ENABLE_DND_MODE,
            -> return listOf(Permission.ACCESS_NOTIFICATION_POLICY)

            SystemActionId.TOGGLE_AUTO_ROTATE,
            SystemActionId.ENABLE_AUTO_ROTATE,
            SystemActionId.DISABLE_AUTO_ROTATE,
            SystemActionId.PORTRAIT_MODE,
            SystemActionId.LANDSCAPE_MODE,
            SystemActionId.SWITCH_ORIENTATION,
            SystemActionId.CYCLE_ROTATIONS,
            -> return listOf(Permission.WRITE_SETTINGS)

            SystemActionId.TOGGLE_AUTO_BRIGHTNESS,
            SystemActionId.ENABLE_AUTO_BRIGHTNESS,
            SystemActionId.DISABLE_AUTO_BRIGHTNESS,
            SystemActionId.INCREASE_BRIGHTNESS,
            SystemActionId.DECREASE_BRIGHTNESS,
            -> return listOf(Permission.WRITE_SETTINGS)

            SystemActionId.TOGGLE_FLASHLIGHT,
            SystemActionId.ENABLE_FLASHLIGHT,
            SystemActionId.DISABLE_FLASHLIGHT,
            -> return listOf(Permission.CAMERA)

            SystemActionId.ENABLE_NFC,
            SystemActionId.DISABLE_NFC,
            SystemActionId.TOGGLE_NFC,
            -> return listOf(Permission.ROOT)

            SystemActionId.SHOW_KEYBOARD_PICKER ->
                if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O_MR1..Build.VERSION_CODES.P) {
                    return listOf(Permission.ROOT)
                }

            SystemActionId.SWITCH_KEYBOARD -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                return listOf(Permission.WRITE_SECURE_SETTINGS)
            }

            SystemActionId.TOGGLE_AIRPLANE_MODE,
            SystemActionId.ENABLE_AIRPLANE_MODE,
            SystemActionId.DISABLE_AIRPLANE_MODE,
            -> Permission.ROOT

            SystemActionId.SCREENSHOT -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return listOf(Permission.ROOT)
            }

            SystemActionId.LOCK_DEVICE -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return listOf(Permission.ROOT)
            }

            SystemActionId.SECURE_LOCK_DEVICE -> return listOf(Permission.DEVICE_ADMIN)
            SystemActionId.POWER_ON_OFF_DEVICE -> return listOf(Permission.ROOT)
        }

        return emptyList()
    }
}