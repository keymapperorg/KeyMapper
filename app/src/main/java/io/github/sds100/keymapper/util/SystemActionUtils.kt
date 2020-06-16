package io.github.sds100.keymapper.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Option
import io.github.sds100.keymapper.data.model.SystemActionDef
import io.github.sds100.keymapper.util.SystemAction.CATEGORY_AIRPLANE_MODE
import io.github.sds100.keymapper.util.SystemAction.CATEGORY_BLUETOOTH
import io.github.sds100.keymapper.util.SystemAction.CATEGORY_BRIGHTNESS
import io.github.sds100.keymapper.util.SystemAction.CATEGORY_FLASHLIGHT
import io.github.sds100.keymapper.util.SystemAction.CATEGORY_KEYBOARD
import io.github.sds100.keymapper.util.SystemAction.CATEGORY_MEDIA
import io.github.sds100.keymapper.util.SystemAction.CATEGORY_MOBILE_DATA
import io.github.sds100.keymapper.util.SystemAction.CATEGORY_NAVIGATION
import io.github.sds100.keymapper.util.SystemAction.CATEGORY_NFC
import io.github.sds100.keymapper.util.SystemAction.CATEGORY_OTHER
import io.github.sds100.keymapper.util.SystemAction.CATEGORY_SCREEN_ROTATION
import io.github.sds100.keymapper.util.SystemAction.CATEGORY_STATUS_BAR
import io.github.sds100.keymapper.util.SystemAction.CATEGORY_VOLUME
import io.github.sds100.keymapper.util.SystemAction.CATEGORY_WIFI
import io.github.sds100.keymapper.util.SystemAction.COLLAPSE_STATUS_BAR
import io.github.sds100.keymapper.util.SystemAction.CONSUME_KEY_EVENT
import io.github.sds100.keymapper.util.SystemAction.DECREASE_BRIGHTNESS
import io.github.sds100.keymapper.util.SystemAction.DISABLE_AIRPLANE_MODE
import io.github.sds100.keymapper.util.SystemAction.DISABLE_AUTO_BRIGHTNESS
import io.github.sds100.keymapper.util.SystemAction.DISABLE_AUTO_ROTATE
import io.github.sds100.keymapper.util.SystemAction.DISABLE_BLUETOOTH
import io.github.sds100.keymapper.util.SystemAction.DISABLE_MOBILE_DATA
import io.github.sds100.keymapper.util.SystemAction.DISABLE_NFC
import io.github.sds100.keymapper.util.SystemAction.DISABLE_WIFI
import io.github.sds100.keymapper.util.SystemAction.DISABLE_WIFI_ROOT
import io.github.sds100.keymapper.util.SystemAction.ENABLE_AIRPLANE_MODE
import io.github.sds100.keymapper.util.SystemAction.ENABLE_AUTO_BRIGHTNESS
import io.github.sds100.keymapper.util.SystemAction.ENABLE_AUTO_ROTATE
import io.github.sds100.keymapper.util.SystemAction.ENABLE_BLUETOOTH
import io.github.sds100.keymapper.util.SystemAction.ENABLE_MOBILE_DATA
import io.github.sds100.keymapper.util.SystemAction.ENABLE_NFC
import io.github.sds100.keymapper.util.SystemAction.ENABLE_WIFI
import io.github.sds100.keymapper.util.SystemAction.ENABLE_WIFI_ROOT
import io.github.sds100.keymapper.util.SystemAction.EXPAND_NOTIFICATION_DRAWER
import io.github.sds100.keymapper.util.SystemAction.EXPAND_QUICK_SETTINGS
import io.github.sds100.keymapper.util.SystemAction.FAST_FORWARD
import io.github.sds100.keymapper.util.SystemAction.GO_BACK
import io.github.sds100.keymapper.util.SystemAction.GO_HOME
import io.github.sds100.keymapper.util.SystemAction.HIDE_KEYBOARD
import io.github.sds100.keymapper.util.SystemAction.INCREASE_BRIGHTNESS
import io.github.sds100.keymapper.util.SystemAction.LANDSCAPE_MODE
import io.github.sds100.keymapper.util.SystemAction.LOCK_DEVICE
import io.github.sds100.keymapper.util.SystemAction.LOCK_DEVICE_ROOT
import io.github.sds100.keymapper.util.SystemAction.MOVE_CURSOR_TO_END
import io.github.sds100.keymapper.util.SystemAction.NEXT_TRACK
import io.github.sds100.keymapper.util.SystemAction.OPEN_CAMERA
import io.github.sds100.keymapper.util.SystemAction.OPEN_DEVICE_ASSISTANT
import io.github.sds100.keymapper.util.SystemAction.OPEN_MENU
import io.github.sds100.keymapper.util.SystemAction.OPEN_RECENTS
import io.github.sds100.keymapper.util.SystemAction.OPEN_SETTINGS
import io.github.sds100.keymapper.util.SystemAction.OPEN_VOICE_ASSISTANT
import io.github.sds100.keymapper.util.SystemAction.PAUSE_MEDIA
import io.github.sds100.keymapper.util.SystemAction.PLAY_PAUSE_MEDIA
import io.github.sds100.keymapper.util.SystemAction.PORTRAIT_MODE
import io.github.sds100.keymapper.util.SystemAction.PREVIOUS_TRACK
import io.github.sds100.keymapper.util.SystemAction.REWIND
import io.github.sds100.keymapper.util.SystemAction.SCREENSHOT
import io.github.sds100.keymapper.util.SystemAction.SECURE_LOCK_DEVICE
import io.github.sds100.keymapper.util.SystemAction.SHOW_KEYBOARD
import io.github.sds100.keymapper.util.SystemAction.SHOW_KEYBOARD_PICKER
import io.github.sds100.keymapper.util.SystemAction.SHOW_KEYBOARD_PICKER_ROOT
import io.github.sds100.keymapper.util.SystemAction.SHOW_POWER_MENU
import io.github.sds100.keymapper.util.SystemAction.SWITCH_KEYBOARD
import io.github.sds100.keymapper.util.SystemAction.SWITCH_ORIENTATION
import io.github.sds100.keymapper.util.SystemAction.TOGGLE_AIRPLANE_MODE
import io.github.sds100.keymapper.util.SystemAction.TOGGLE_AUTO_BRIGHTNESS
import io.github.sds100.keymapper.util.SystemAction.TOGGLE_AUTO_ROTATE
import io.github.sds100.keymapper.util.SystemAction.TOGGLE_BLUETOOTH
import io.github.sds100.keymapper.util.SystemAction.TOGGLE_KEYBOARD
import io.github.sds100.keymapper.util.SystemAction.TOGGLE_MOBILE_DATA
import io.github.sds100.keymapper.util.SystemAction.TOGGLE_NFC
import io.github.sds100.keymapper.util.SystemAction.TOGGLE_SPLIT_SCREEN
import io.github.sds100.keymapper.util.SystemAction.TOGGLE_WIFI
import io.github.sds100.keymapper.util.SystemAction.TOGGLE_WIFI_ROOT
import io.github.sds100.keymapper.util.SystemAction.VOLUME_MUTE
import io.github.sds100.keymapper.util.SystemAction.VOLUME_TOGGLE_MUTE
import io.github.sds100.keymapper.util.SystemAction.VOLUME_UNMUTE
import io.github.sds100.keymapper.util.result.*

/**
 * Created by sds100 on 01/08/2018.
 */

object SystemActionUtils {

    /**
     * Maps system action category ids to the string resource of their label
     */
    val CATEGORY_LABEL_MAP = mapOf(
        CATEGORY_WIFI to R.string.system_action_cat_wifi,
        CATEGORY_BLUETOOTH to R.string.system_action_cat_bluetooth,
        CATEGORY_MOBILE_DATA to R.string.system_action_cat_mobile_data,
        CATEGORY_NAVIGATION to R.string.system_action_cat_navigation,
        CATEGORY_SCREEN_ROTATION to R.string.system_action_cat_screen_rotation,
        CATEGORY_VOLUME to R.string.system_action_cat_volume,
        CATEGORY_BRIGHTNESS to R.string.system_action_cat_brightness,
        CATEGORY_STATUS_BAR to R.string.system_action_cat_status_bar,
        CATEGORY_MEDIA to R.string.system_action_cat_media,
        CATEGORY_FLASHLIGHT to R.string.system_action_cat_flashlight,
        CATEGORY_KEYBOARD to R.string.system_action_cat_keyboard,
        CATEGORY_NFC to R.string.system_action_cat_nfc,
        CATEGORY_AIRPLANE_MODE to R.string.system_action_cat_airplane_mode,
        CATEGORY_OTHER to R.string.system_action_cat_other
    )

    /**
     * A sorted list of system action definitions
     */
    @SuppressLint("NewApi")
    private val SYSTEM_ACTION_DEFINITIONS = listOf(

        //NAVIGATION
        SystemActionDef(
            id = GO_BACK,
            category = CATEGORY_NAVIGATION,
            iconRes = R.drawable.ic_baseline_arrow_back_24,
            descriptionRes = R.string.action_go_back
        ),
        SystemActionDef(
            id = GO_HOME,
            category = CATEGORY_NAVIGATION,
            iconRes = R.drawable.ic_outline_home_24,
            descriptionRes = R.string.action_go_home
        ),
        SystemActionDef(
            id = OPEN_RECENTS,
            category = CATEGORY_NAVIGATION,
            descriptionRes = R.string.action_open_recents
        ),
        SystemActionDef(
            id = OPEN_MENU,
            category = CATEGORY_NAVIGATION,
            iconRes = R.drawable.ic_outline_more_vert_24,
            descriptionRes = R.string.action_open_menu
        ),
        SystemActionDef(
            id = TOGGLE_SPLIT_SCREEN,
            category = CATEGORY_NAVIGATION,
            descriptionRes = R.string.action_toggle_split_screen,
            minApi = Build.VERSION_CODES.N
        ),
        //NAVIGATION

        //STATUS BAR
        SystemActionDef(
            id = EXPAND_NOTIFICATION_DRAWER,
            category = CATEGORY_STATUS_BAR,
            descriptionRes = R.string.action_expand_notification_drawer
        ),
        SystemActionDef(
            id = EXPAND_QUICK_SETTINGS,
            category = CATEGORY_STATUS_BAR,
            descriptionRes = R.string.action_expand_quick_settings
        ),
        SystemActionDef(
            id = COLLAPSE_STATUS_BAR,
            category = CATEGORY_STATUS_BAR,
            descriptionRes = R.string.action_collapse_status_bar
        ),
        //STATUS BAR

        //WIFI
        SystemActionDef(
            id = TOGGLE_WIFI,
            category = CATEGORY_WIFI,
            iconRes = R.drawable.ic_outline_wifi_24,
            maxApi = Build.VERSION_CODES.P,
            descriptionRes = R.string.action_toggle_wifi
        ),
        SystemActionDef(
            id = ENABLE_WIFI,
            category = CATEGORY_WIFI,
            maxApi = Build.VERSION_CODES.P,
            iconRes = R.drawable.ic_outline_wifi_24,
            descriptionRes = R.string.action_enable_wifi
        ),
        SystemActionDef(
            id = DISABLE_WIFI,
            category = CATEGORY_WIFI,
            maxApi = Build.VERSION_CODES.P,
            iconRes = R.drawable.ic_outline_wifi_off_24,
            descriptionRes = R.string.action_disable_wifi
        ),

        SystemActionDef(
            id = TOGGLE_WIFI_ROOT,
            category = CATEGORY_WIFI,
            minApi = Build.VERSION_CODES.Q,
            iconRes = R.drawable.ic_outline_wifi_24,
            descriptionRes = R.string.action_toggle_wifi_root,
            permissions = arrayOf(Constants.PERMISSION_ROOT)
        ),

        SystemActionDef(
            id = ENABLE_WIFI_ROOT,
            category = CATEGORY_WIFI,
            minApi = Build.VERSION_CODES.Q,
            iconRes = R.drawable.ic_outline_wifi_24,
            descriptionRes = R.string.action_enable_wifi_root,
            permissions = arrayOf(Constants.PERMISSION_ROOT)
        ),

        SystemActionDef(
            id = DISABLE_WIFI_ROOT,
            category = CATEGORY_WIFI,
            minApi = Build.VERSION_CODES.Q,
            iconRes = R.drawable.ic_outline_wifi_off_24,
            descriptionRes = R.string.action_toggle_wifi_root,
            permissions = arrayOf(Constants.PERMISSION_ROOT)
        ),
        //WIFI

        //BLUETOOTH
        SystemActionDef(
            id = TOGGLE_BLUETOOTH,
            category = CATEGORY_BLUETOOTH,
            iconRes = R.drawable.ic_outline_bluetooth_24,
            descriptionRes = R.string.action_toggle_bluetooth
        ),
        SystemActionDef(
            id = ENABLE_BLUETOOTH,
            category = CATEGORY_BLUETOOTH,
            iconRes = R.drawable.ic_outline_bluetooth_24,
            descriptionRes = R.string.action_enable_bluetooth
        ),
        SystemActionDef(
            id = DISABLE_BLUETOOTH,
            category = CATEGORY_BLUETOOTH,
            iconRes = R.drawable.ic_outline_bluetooth_disabled_24,
            descriptionRes = R.string.action_disable_bluetooth
        ),
        //BLUETOOTH

        //MOBILE DATA REQUIRES ROOT!
        SystemActionDef(
            id = TOGGLE_MOBILE_DATA,
            category = CATEGORY_MOBILE_DATA,
            iconRes = R.drawable.ic_outline_signal_cellular_4_bar_24,
            /*needs READ_PHONE_STATE permission so it can check whether mobile data is enabled. On some devices
            * it seems to need this permission.*/
            permissions = arrayOf(Constants.PERMISSION_ROOT, Manifest.permission.READ_PHONE_STATE),
            descriptionRes = R.string.action_toggle_mobile_data
        ),
        SystemActionDef(
            id = ENABLE_MOBILE_DATA,
            category = CATEGORY_MOBILE_DATA,
            iconRes = R.drawable.ic_outline_signal_cellular_4_bar_24,
            permissions = arrayOf(Constants.PERMISSION_ROOT),
            descriptionRes = R.string.action_enable_mobile_data
        ),
        SystemActionDef(
            id = DISABLE_MOBILE_DATA,
            category = CATEGORY_MOBILE_DATA,
            iconRes = R.drawable.ic_outline_signal_cellular_off_24,
            permissions = arrayOf(Constants.PERMISSION_ROOT),
            descriptionRes = R.string.action_disable_mobile_data
        ),
        //MOBILE DATA

        //MEDIA
        SystemActionDef(
            id = PLAY_PAUSE_MEDIA,
            category = CATEGORY_MEDIA,
            iconRes = R.drawable.ic_play_pause_24dp,
            descriptionRes = R.string.action_play_pause_media
        ),
        SystemActionDef(
            id = PAUSE_MEDIA,
            category = CATEGORY_MEDIA,
            iconRes = R.drawable.ic_outline_pause_24,
            descriptionRes = R.string.action_pause_media
        ),
        SystemActionDef(
            id = PLAY_PAUSE_MEDIA,
            category = CATEGORY_MEDIA,
            iconRes = R.drawable.ic_outline_play_arrow_24,
            descriptionRes = R.string.action_play_media
        ),
        SystemActionDef(
            id = NEXT_TRACK,
            category = CATEGORY_MEDIA,
            iconRes = R.drawable.ic_outline_skip_next_24,
            descriptionRes = R.string.action_next_track
        ),
        SystemActionDef(
            id = PREVIOUS_TRACK,
            category = CATEGORY_MEDIA,
            iconRes = R.drawable.ic_outline_skip_previous_24,
            descriptionRes = R.string.action_previous_track
        ),
        SystemActionDef(
            id = FAST_FORWARD,
            category = CATEGORY_MEDIA,
            iconRes = R.drawable.ic_outline_fast_forward_24,
            descriptionRes = R.string.action_fast_forward,
            messageOnSelection = R.string.action_fast_forward_message
        ),
        SystemActionDef(
            id = REWIND,
            category = CATEGORY_MEDIA,
            iconRes = R.drawable.ic_outline_fast_rewind_24,
            descriptionRes = R.string.action_rewind,
            messageOnSelection = R.string.action_rewind_message
        ),
        //MEDIA

        //VOLUME
        SystemActionDef(
            id = SystemAction.VOLUME_UP,
            category = CATEGORY_VOLUME,
            iconRes = R.drawable.ic_outline_volume_up_24,
            descriptionRes = R.string.action_volume_up,
            permissions = arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
        ),
        SystemActionDef(
            id = SystemAction.VOLUME_DOWN,
            category = CATEGORY_VOLUME,
            iconRes = R.drawable.ic_outline_volume_down_24,
            descriptionRes = R.string.action_volume_down,
            permissions = arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
        ),
        SystemActionDef(
            id = SystemAction.VOLUME_INCREASE_STREAM,
            category = CATEGORY_VOLUME,
            iconRes = R.drawable.ic_outline_volume_up_24,
            descriptionRes = R.string.action_increase_stream,
            permissions = arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY),
            descriptionFormattedRes = R.string.action_increase_stream_formatted,
            options = Option.STREAMS
        ),
        SystemActionDef(
            id = SystemAction.VOLUME_DECREASE_STREAM,
            category = CATEGORY_VOLUME,
            iconRes = R.drawable.ic_outline_volume_down_24,
            descriptionRes = R.string.action_decrease_stream,
            descriptionFormattedRes = R.string.action_increase_stream_formatted,
            options = Option.STREAMS,
            permissions = arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
        ),
        SystemActionDef(
            id = SystemAction.VOLUME_SHOW_DIALOG,
            category = CATEGORY_VOLUME,
            descriptionRes = R.string.action_volume_show_dialog
        ),
        SystemActionDef(
            id = SystemAction.CYCLE_RINGER_MODE,
            category = CATEGORY_VOLUME,
            descriptionRes = R.string.action_cycle_ringer_mode,
            permissions = arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
        ),
        SystemActionDef(
            id = SystemAction.CYCLE_VIBRATE_RING,
            category = CATEGORY_VOLUME,
            descriptionRes = R.string.action_cycle_vibrate_ring,
            permissions = arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
        ),
        SystemActionDef(id = SystemAction.CHANGE_RINGER_MODE,
            category = CATEGORY_VOLUME,
            descriptionRes = R.string.action_change_ringer_mode,
            descriptionFormattedRes = R.string.action_change_ringer_mode_formatted,
            permissions = arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY),
            options = listOf(
                Option.RINGER_MODE_NORMAL,
                Option.RINGER_MODE_VIBRATE,
                Option.RINGER_MODE_SILENT
            )
        ),

        //Require Marshmallow and higher
        SystemActionDef(
            id = VOLUME_MUTE,
            category = CATEGORY_VOLUME,
            minApi = Build.VERSION_CODES.M,
            iconRes = R.drawable.ic_outline_volume_mute_24,
            descriptionRes = R.string.action_volume_mute,
            permissions = arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
        ),
        SystemActionDef(
            id = VOLUME_UNMUTE,
            category = CATEGORY_VOLUME,
            minApi = Build.VERSION_CODES.M,
            iconRes = R.drawable.ic_outline_volume_up_24,
            descriptionRes = R.string.action_volume_unmute,
            permissions = arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
        ),
        SystemActionDef(
            id = VOLUME_TOGGLE_MUTE,
            category = CATEGORY_VOLUME,
            minApi = Build.VERSION_CODES.M,
            iconRes = R.drawable.ic_outline_volume_mute_24,
            descriptionRes = R.string.action_toggle_mute,
            permissions = arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
        ),

        SystemActionDef(id = SystemAction.TOGGLE_DND_MODE,
            category = CATEGORY_VOLUME,
            minApi = Build.VERSION_CODES.M,
            iconRes = R.drawable.dnd_circle_outline,
            descriptionRes = R.string.action_toggle_dnd_mode,
            descriptionFormattedRes = R.string.action_toggle_dnd_mode_formatted,
            permissions = arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY),
            options = Option.DND_MODES),

        SystemActionDef(id = SystemAction.ENABLE_DND_MODE,
            category = CATEGORY_VOLUME,
            minApi = Build.VERSION_CODES.M,
            iconRes = R.drawable.dnd_circle_outline,
            descriptionRes = R.string.action_enable_dnd_mode,
            descriptionFormattedRes = R.string.action_enable_dnd_mode_formatted,
            permissions = arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY),
            options = Option.DND_MODES),

        SystemActionDef(id = SystemAction.DISABLE_DND_MODE,
            category = CATEGORY_VOLUME,
            minApi = Build.VERSION_CODES.M,
            iconRes = R.drawable.dnd_circle_off_outline,
            descriptionRes = R.string.action_disable_dnd_mode,
            permissions = arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY)),
        //VOLUME

        //SCREEN ORIENTATION
        SystemActionDef(
            id = TOGGLE_AUTO_ROTATE,
            category = CATEGORY_SCREEN_ROTATION,
            permissions = arrayOf(Manifest.permission.WRITE_SETTINGS),
            iconRes = R.drawable.ic_outline_screen_rotation_24,
            descriptionRes = R.string.action_toggle_auto_rotate
        ),
        SystemActionDef(
            id = ENABLE_AUTO_ROTATE,
            category = CATEGORY_SCREEN_ROTATION,
            permissions = arrayOf(Manifest.permission.WRITE_SETTINGS),
            iconRes = R.drawable.ic_outline_screen_rotation_24,
            descriptionRes = R.string.action_enable_auto_rotate
        ),
        SystemActionDef(
            id = DISABLE_AUTO_ROTATE,
            category = CATEGORY_SCREEN_ROTATION,
            permissions = arrayOf(Manifest.permission.WRITE_SETTINGS),
            iconRes = R.drawable.ic_outline_screen_lock_rotation_24,
            descriptionRes = R.string.action_disable_auto_rotate
        ),
        SystemActionDef(
            id = PORTRAIT_MODE,
            category = CATEGORY_SCREEN_ROTATION,
            permissions = arrayOf(Manifest.permission.WRITE_SETTINGS),
            iconRes = R.drawable.ic_outline_stay_current_portrait_24,
            descriptionRes = R.string.action_portrait_mode
        ),
        SystemActionDef(
            id = LANDSCAPE_MODE,
            category = CATEGORY_SCREEN_ROTATION,
            permissions = arrayOf(Manifest.permission.WRITE_SETTINGS),
            iconRes = R.drawable.ic_outline_stay_current_landscape_24,
            descriptionRes = R.string.action_landscape_mode
        ),
        SystemActionDef(
            id = SWITCH_ORIENTATION,
            category = CATEGORY_SCREEN_ROTATION,
            permissions = arrayOf(Manifest.permission.WRITE_SETTINGS),
            iconRes = R.drawable.ic_outline_screen_rotation_24,
            descriptionRes = R.string.action_switch_orientation
        ),
        //SCREEN ORIENTATION

        //BRIGHTNESS
        SystemActionDef(
            id = TOGGLE_AUTO_BRIGHTNESS,
            category = CATEGORY_BRIGHTNESS,
            iconRes = R.drawable.ic_outline_brightness_auto_24,
            descriptionRes = R.string.action_toggle_auto_brightness,
            permissions = arrayOf(Manifest.permission.WRITE_SETTINGS)
        ),
        SystemActionDef(
            id = ENABLE_AUTO_BRIGHTNESS,
            category = CATEGORY_BRIGHTNESS,
            iconRes = R.drawable.ic_outline_brightness_auto_24,
            descriptionRes = R.string.action_enable_auto_brightness,
            permissions = arrayOf(Manifest.permission.WRITE_SETTINGS)
        ),
        SystemActionDef(
            id = DISABLE_AUTO_BRIGHTNESS,
            category = CATEGORY_BRIGHTNESS,
            iconRes = R.drawable.ic_disable_brightness_auto_24dp,
            descriptionRes = R.string.action_disable_auto_brightness,
            permissions = arrayOf(Manifest.permission.WRITE_SETTINGS)
        ),
        SystemActionDef(
            id = INCREASE_BRIGHTNESS,
            category = CATEGORY_BRIGHTNESS,
            iconRes = R.drawable.ic_outline_brightness_high_24,
            descriptionRes = R.string.action_increase_brightness,
            permissions = arrayOf(Manifest.permission.WRITE_SETTINGS)
        ),
        SystemActionDef(
            id = DECREASE_BRIGHTNESS,
            category = CATEGORY_BRIGHTNESS,
            iconRes = R.drawable.ic_outline_brightness_low_24,
            descriptionRes = R.string.action_decrease_brightness,
            permissions = arrayOf(Manifest.permission.WRITE_SETTINGS)
        ),

        //FLASHLIGHT
        SystemActionDef(
            id = SystemAction.TOGGLE_FLASHLIGHT,
            category = CATEGORY_FLASHLIGHT,
            permissions = arrayOf(Manifest.permission.CAMERA),
            features = arrayOf(PackageManager.FEATURE_CAMERA_FLASH),
            minApi = Build.VERSION_CODES.M,
            iconRes = R.drawable.ic_flashlight,
            descriptionRes = R.string.action_toggle_flashlight,
            descriptionFormattedRes = R.string.action_toggle_flashlight_formatted,
            options = Option.LENSES
        ),
        SystemActionDef(
            id = SystemAction.ENABLE_FLASHLIGHT,
            category = CATEGORY_FLASHLIGHT,
            permissions = arrayOf(Manifest.permission.CAMERA),
            features = arrayOf(PackageManager.FEATURE_CAMERA_FLASH),
            minApi = Build.VERSION_CODES.M,
            iconRes = R.drawable.ic_flashlight,
            descriptionRes = R.string.action_enable_flashlight,
            descriptionFormattedRes = R.string.action_toggle_flashlight_formatted,
            options = Option.LENSES
        ),
        SystemActionDef(
            id = SystemAction.DISABLE_FLASHLIGHT,
            category = CATEGORY_FLASHLIGHT,
            permissions = arrayOf(Manifest.permission.CAMERA),
            features = arrayOf(PackageManager.FEATURE_CAMERA_FLASH),
            minApi = Build.VERSION_CODES.M,
            iconRes = R.drawable.ic_flashlight_off,
            descriptionRes = R.string.action_disable_flashlight,
            descriptionFormattedRes = R.string.action_toggle_flashlight_formatted,
            options = Option.LENSES
        ),

        //NFC
        SystemActionDef(
            id = ENABLE_NFC,
            category = CATEGORY_NFC,
            iconRes = R.drawable.ic_outline_nfc_24,
            permissions = arrayOf(Constants.PERMISSION_ROOT),
            features = arrayOf(PackageManager.FEATURE_NFC),
            descriptionRes = R.string.action_nfc_enable
        ),
        SystemActionDef(
            id = DISABLE_NFC,
            category = CATEGORY_NFC,
            features = arrayOf(PackageManager.FEATURE_NFC),
            iconRes = R.drawable.ic_nfc_off,
            permissions = arrayOf(Constants.PERMISSION_ROOT),
            descriptionRes = R.string.action_nfc_disable
        ),
        SystemActionDef(
            id = TOGGLE_NFC,
            category = CATEGORY_NFC,
            features = arrayOf(PackageManager.FEATURE_NFC),
            iconRes = R.drawable.ic_outline_nfc_24,
            permissions = arrayOf(Constants.PERMISSION_ROOT),
            descriptionRes = R.string.action_nfc_toggle
        ),

        //KEYBOARD
        SystemActionDef(id = MOVE_CURSOR_TO_END,
            category = CATEGORY_KEYBOARD,
            iconRes = R.drawable.ic_cursor,
            messageOnSelection = R.string.action_move_to_end_of_text_message,
            descriptionRes = R.string.action_move_to_end_of_text),

        SystemActionDef(id = TOGGLE_KEYBOARD,
            category = CATEGORY_KEYBOARD,
            minApi = Build.VERSION_CODES.N,
            iconRes = R.drawable.ic_notification_keyboard,
            messageOnSelection = R.string.action_toggle_keyboard_message,
            descriptionRes = R.string.action_toggle_keyboard),

        SystemActionDef(id = SHOW_KEYBOARD,
            category = CATEGORY_KEYBOARD,
            minApi = Build.VERSION_CODES.N,
            iconRes = R.drawable.ic_notification_keyboard,
            messageOnSelection = R.string.action_toggle_keyboard_message,
            descriptionRes = R.string.action_show_keyboard),

        SystemActionDef(id = HIDE_KEYBOARD,
            category = CATEGORY_KEYBOARD,
            minApi = Build.VERSION_CODES.N,
            iconRes = R.drawable.ic_outline_keyboard_hide_24,
            messageOnSelection = R.string.action_toggle_keyboard_message,
            descriptionRes = R.string.action_hide_keyboard),

        SystemActionDef(id = SHOW_KEYBOARD_PICKER,
            category = CATEGORY_KEYBOARD,
            iconRes = R.drawable.ic_notification_keyboard,
            maxApi = Build.VERSION_CODES.O,
            descriptionRes = R.string.action_show_keyboard_picker),

        SystemActionDef(id = SHOW_KEYBOARD_PICKER_ROOT,
            category = CATEGORY_KEYBOARD,
            iconRes = R.drawable.ic_notification_keyboard,
            permissions = arrayOf(Constants.PERMISSION_ROOT),
            minApi = Build.VERSION_CODES.O_MR1,
            maxApi = Build.VERSION_CODES.P,
            descriptionRes = R.string.action_show_keyboard_picker_root),

        SystemActionDef(id = SWITCH_KEYBOARD,
            category = CATEGORY_KEYBOARD,
            iconRes = R.drawable.ic_notification_keyboard,
            permissions = arrayOf(Constants.PERMISSION_ROOT),
            descriptionRes = R.string.action_switch_keyboard,
            descriptionFormattedRes = R.string.action_switch_keyboard_formatted,
            getOptions = {
                KeyboardUtils.getInputMethodIds()
            }
        ),

        //AIRPLANE MODE
        SystemActionDef(id = TOGGLE_AIRPLANE_MODE,
            category = CATEGORY_AIRPLANE_MODE,
            iconRes = R.drawable.ic_outline_airplanemode_active_24,
            descriptionRes = R.string.action_toggle_airplane_mode,
            permissions = arrayOf(Constants.PERMISSION_ROOT)
        ),
        SystemActionDef(id = ENABLE_AIRPLANE_MODE,
            category = CATEGORY_AIRPLANE_MODE,
            iconRes = R.drawable.ic_outline_airplanemode_active_24,
            descriptionRes = R.string.action_enable_airplane_mode,
            permissions = arrayOf(Constants.PERMISSION_ROOT)
        ),
        SystemActionDef(id = DISABLE_AIRPLANE_MODE,
            category = CATEGORY_AIRPLANE_MODE,
            iconRes = R.drawable.ic_outline_airplanemode_inactive_24,
            descriptionRes = R.string.action_disable_airplane_mode,
            permissions = arrayOf(Constants.PERMISSION_ROOT)
        ),

        //OTHER
        SystemActionDef(
            id = SCREENSHOT,
            category = CATEGORY_OTHER,
            minApi = Build.VERSION_CODES.P,
            iconRes = R.drawable.ic_outline_fullscreen_24,
            descriptionRes = R.string.action_screenshot
        ),
        SystemActionDef(
            id = OPEN_VOICE_ASSISTANT,
            category = CATEGORY_OTHER,
            iconRes = R.drawable.ic_outline_assistant_24,
            descriptionRes = R.string.action_open_assistant
        ),
        SystemActionDef(
            id = OPEN_DEVICE_ASSISTANT,
            category = CATEGORY_OTHER,
            iconRes = R.drawable.ic_outline_assistant_24,
            descriptionRes = R.string.action_open_device_assistant
        ),
        SystemActionDef(
            id = OPEN_CAMERA,
            category = CATEGORY_OTHER,
            iconRes = R.drawable.ic_outline_camera_alt_24,
            descriptionRes = R.string.action_open_camera
        ),
        SystemActionDef(
            id = LOCK_DEVICE,
            category = CATEGORY_OTHER,
            iconRes = R.drawable.ic_outline_lock_24,
            descriptionRes = R.string.action_lock_device,
            minApi = Build.VERSION_CODES.P
        ),
        SystemActionDef(
            id = LOCK_DEVICE_ROOT,
            category = CATEGORY_OTHER,
            iconRes = R.drawable.ic_outline_lock_24,
            descriptionRes = R.string.action_lock_device_root,
            maxApi = Build.VERSION_CODES.O_MR1,
            permissions = arrayOf(Constants.PERMISSION_ROOT)
        ),
        SystemActionDef(
            id = SECURE_LOCK_DEVICE,
            category = CATEGORY_OTHER,
            iconRes = R.drawable.ic_outline_lock_24,
            descriptionRes = R.string.action_secure_lock_device,
            features = arrayOf(PackageManager.FEATURE_DEVICE_ADMIN),
            permissions = arrayOf(Manifest.permission.BIND_DEVICE_ADMIN),
            messageOnSelection = R.string.action_secure_lock_device_message
        ),
        SystemActionDef(
            id = CONSUME_KEY_EVENT,
            category = CATEGORY_OTHER,
            descriptionRes = R.string.action_consume_keyevent
        ),
        SystemActionDef(
            id = OPEN_SETTINGS,
            category = CATEGORY_OTHER,
            descriptionRes = R.string.action_open_settings,
            iconRes = R.drawable.ic_outline_settings_applications_24
        ),
        SystemActionDef(
            id = SHOW_POWER_MENU,
            category = CATEGORY_OTHER,
            descriptionRes = R.string.action_show_power_menu,
            iconRes = R.drawable.ic_outline_power_settings_new_24,
            minApi = Build.VERSION_CODES.LOLLIPOP
        )
    )

    /**
     * Get all the system actions which are supported by the system.
     */
    fun getSupportedSystemActions(ctx: Context) = SYSTEM_ACTION_DEFINITIONS.filter { it.isSupported(ctx) is Success }

    fun getUnsupportedSystemActions(ctx: Context) = SYSTEM_ACTION_DEFINITIONS.filter { it.isSupported(ctx) !is Success }

    fun getUnsupportedSystemActionsWithReasons(ctx: Context): Map<SystemActionDef, Failure> =
        SYSTEM_ACTION_DEFINITIONS.filter { it.isSupported(ctx) is Failure }.map {
            it to (it.isSupported(ctx) as Failure)
        }.toMap()

    /**
     * @return null if the action is supported.
     */
    fun SystemActionDef.isSupported(ctx: Context): Result<SystemActionDef> {
        if (Build.VERSION.SDK_INT < minApi) {
            return SdkVersionTooLow(minApi)
        }

        for (feature in features) {
            if (!ctx.packageManager.hasSystemFeature(feature)) {
                return FeatureUnavailable(feature)
            }
        }

        if (Build.VERSION.SDK_INT > maxApi) {
            return SdkVersionTooHigh(maxApi)
        }

        val options = getOptions()

        if (options is Failure && options !is OptionsNotRequired) {
            return options
        }

        return Success(this)
    }

    fun getSystemActionDef(id: String): Result<SystemActionDef> {
        val systemActionDef = SYSTEM_ACTION_DEFINITIONS.find { it.id == id } ?: return SystemActionNotFound(id)

        return Success(systemActionDef)
    }

    fun SystemActionDef.getDescriptionWithOption(ctx: Context, optionText: String): String {
        descriptionFormattedRes
            ?: throw Exception("System action $id has options and doesn't have a formatted description")

        return ctx.str(descriptionFormattedRes, optionText)
    }
}