package io.github.sds100.keymapper.Utils

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.SystemAction.CATEGORY_BLUETOOTH
import io.github.sds100.keymapper.SystemAction.CATEGORY_BRIGHTNESS
import io.github.sds100.keymapper.SystemAction.CATEGORY_FLASHLIGHT
import io.github.sds100.keymapper.SystemAction.CATEGORY_MEDIA
import io.github.sds100.keymapper.SystemAction.CATEGORY_MOBILE_DATA
import io.github.sds100.keymapper.SystemAction.CATEGORY_NAVIGATION
import io.github.sds100.keymapper.SystemAction.CATEGORY_OTHER
import io.github.sds100.keymapper.SystemAction.CATEGORY_SCREEN_ROTATION
import io.github.sds100.keymapper.SystemAction.CATEGORY_STATUS_BAR
import io.github.sds100.keymapper.SystemAction.CATEGORY_VOLUME
import io.github.sds100.keymapper.SystemAction.CATEGORY_WIFI
import io.github.sds100.keymapper.SystemAction.COLLAPSE_STATUS_BAR
import io.github.sds100.keymapper.SystemAction.DECREASE_BRIGHTNESS
import io.github.sds100.keymapper.SystemAction.DISABLE_AUTO_BRIGHTNESS
import io.github.sds100.keymapper.SystemAction.DISABLE_AUTO_ROTATE
import io.github.sds100.keymapper.SystemAction.DISABLE_BLUETOOTH
import io.github.sds100.keymapper.SystemAction.DISABLE_MOBILE_DATA
import io.github.sds100.keymapper.SystemAction.DISABLE_WIFI
import io.github.sds100.keymapper.SystemAction.ENABLE_AUTO_BRIGHTNESS
import io.github.sds100.keymapper.SystemAction.ENABLE_AUTO_ROTATE
import io.github.sds100.keymapper.SystemAction.ENABLE_BLUETOOTH
import io.github.sds100.keymapper.SystemAction.ENABLE_MOBILE_DATA
import io.github.sds100.keymapper.SystemAction.ENABLE_WIFI
import io.github.sds100.keymapper.SystemAction.EXPAND_NOTIFICATION_DRAWER
import io.github.sds100.keymapper.SystemAction.EXPAND_QUICK_SETTINGS
import io.github.sds100.keymapper.SystemAction.GO_BACK
import io.github.sds100.keymapper.SystemAction.GO_HOME
import io.github.sds100.keymapper.SystemAction.INCREASE_BRIGHTNESS
import io.github.sds100.keymapper.SystemAction.LANDSCAPE_MODE
import io.github.sds100.keymapper.SystemAction.NEXT_TRACK
import io.github.sds100.keymapper.SystemAction.OPEN_MENU
import io.github.sds100.keymapper.SystemAction.OPEN_RECENTS
import io.github.sds100.keymapper.SystemAction.PAUSE_MEDIA
import io.github.sds100.keymapper.SystemAction.PLAY_PAUSE_MEDIA
import io.github.sds100.keymapper.SystemAction.PORTRAIT_MODE
import io.github.sds100.keymapper.SystemAction.PREVIOUS_TRACK
import io.github.sds100.keymapper.SystemAction.SCREENSHOT
import io.github.sds100.keymapper.SystemAction.TOGGLE_AUTO_BRIGHTNESS
import io.github.sds100.keymapper.SystemAction.TOGGLE_AUTO_ROTATE
import io.github.sds100.keymapper.SystemAction.TOGGLE_BLUETOOTH
import io.github.sds100.keymapper.SystemAction.TOGGLE_MOBILE_DATA
import io.github.sds100.keymapper.SystemAction.TOGGLE_WIFI
import io.github.sds100.keymapper.SystemAction.VOLUME_MUTE
import io.github.sds100.keymapper.SystemAction.VOLUME_TOGGLE_MUTE
import io.github.sds100.keymapper.SystemAction.VOLUME_UNMUTE
import io.github.sds100.keymapper.Utils.ErrorCodeUtils.ERROR_CODE_SYSTEM_ACTION_NOT_FOUND

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
                    iconRes = R.drawable.ic_arrow_back_black_24dp,
                    descriptionRes = R.string.action_go_back
            ),
            SystemActionDef(
                    id = GO_HOME,
                    category = CATEGORY_NAVIGATION,
                    iconRes = R.drawable.ic_home_black_24dp,
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
                    iconRes = R.drawable.ic_more_vert_black_24dp,
                    permission = Constants.PERMISSION_ROOT,
                    descriptionRes = R.string.action_open_menu
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
                    iconRes = R.drawable.ic_network_wifi_black_24dp,
                    descriptionRes = R.string.action_toggle_wifi
            ),
            SystemActionDef(
                    id = ENABLE_WIFI,
                    category = CATEGORY_WIFI,
                    iconRes = R.drawable.ic_network_wifi_black_24dp,
                    descriptionRes = R.string.action_enable_wifi
            ),
            SystemActionDef(
                    id = DISABLE_WIFI,
                    category = CATEGORY_WIFI,
                    iconRes = R.drawable.ic_signal_wifi_off_black_24dp,
                    descriptionRes = R.string.action_disable_wifi
            ),
            //WIFI

            //BLUETOOTH
            SystemActionDef(
                    id = TOGGLE_BLUETOOTH,
                    category = CATEGORY_BLUETOOTH,
                    iconRes = R.drawable.ic_bluetooth_black_24dp,
                    descriptionRes = R.string.action_toggle_bluetooth
            ),
            SystemActionDef(
                    id = ENABLE_BLUETOOTH,
                    category = CATEGORY_BLUETOOTH,
                    iconRes = R.drawable.ic_bluetooth_black_24dp,
                    descriptionRes = R.string.action_enable_bluetooth
            ),
            SystemActionDef(
                    id = DISABLE_BLUETOOTH,
                    category = CATEGORY_BLUETOOTH,
                    iconRes = R.drawable.ic_bluetooth_disabled_black_24dp,
                    descriptionRes = R.string.action_disable_bluetooth
            ),
            //BLUETOOTH

            //MOBILE DATA REQUIRES ROOT!
            SystemActionDef(
                    id = TOGGLE_MOBILE_DATA,
                    category = CATEGORY_MOBILE_DATA,
                    iconRes = R.drawable.ic_signal,
                    permission = Constants.PERMISSION_ROOT,
                    descriptionRes = R.string.action_toggle_mobile_data
            ),
            SystemActionDef(
                    id = ENABLE_MOBILE_DATA,
                    category = CATEGORY_MOBILE_DATA,
                    iconRes = R.drawable.ic_signal,
                    permission = Constants.PERMISSION_ROOT,
                    descriptionRes = R.string.action_enable_mobile_data
            ),
            SystemActionDef(
                    id = DISABLE_MOBILE_DATA,
                    category = CATEGORY_MOBILE_DATA,
                    iconRes = R.drawable.ic_signal_off,
                    permission = Constants.PERMISSION_ROOT,
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
                    iconRes = R.drawable.ic_pause_black_24dp,
                    descriptionRes = R.string.action_pause_media
            ),
            SystemActionDef(
                    id = PLAY_PAUSE_MEDIA,
                    category = CATEGORY_MEDIA,
                    iconRes = R.drawable.ic_play_arrow_black_24dp,
                    descriptionRes = R.string.action_play_media
            ),
            SystemActionDef(
                    id = NEXT_TRACK,
                    category = CATEGORY_MEDIA,
                    iconRes = R.drawable.ic_skip_next_black_24dp,
                    descriptionRes = R.string.action_next_track
            ),
            SystemActionDef(
                    id = PREVIOUS_TRACK,
                    category = CATEGORY_MEDIA,
                    iconRes = R.drawable.ic_skip_previous_black_24dp,
                    descriptionRes = R.string.action_previous_track
            ),
            //MEDIA

            //VOLUME
            SystemActionDef(
                    id = SystemAction.VOLUME_UP,
                    category = CATEGORY_VOLUME,
                    iconRes = R.drawable.ic_volume_up_black_24dp,
                    descriptionRes = R.string.action_volume_up
            ),
            SystemActionDef(
                    id = SystemAction.VOLUME_DOWN,
                    category = CATEGORY_VOLUME,
                    iconRes = R.drawable.ic_volume_down_black_24dp,
                    descriptionRes = R.string.action_volume_down
            ),
            SystemActionDef(
                    id = SystemAction.VOLUME_SHOW_DIALOG,
                    category = CATEGORY_VOLUME,
                    descriptionRes = R.string.action_volume_show_dialog
            ),

            //Require Marshmallow and higher
            SystemActionDef(
                    id = VOLUME_MUTE,
                    category = CATEGORY_VOLUME,
                    minApi = Build.VERSION_CODES.M,
                    iconRes = R.drawable.ic_volume_mute_black_24dp,
                    descriptionRes = R.string.action_volume_mute
            ),
            SystemActionDef(
                    id = VOLUME_UNMUTE,
                    category = CATEGORY_VOLUME,
                    minApi = Build.VERSION_CODES.M,
                    iconRes = R.drawable.ic_volume_up_black_24dp,
                    descriptionRes = R.string.action_volume_unmute
            ),
            SystemActionDef(
                    id = VOLUME_TOGGLE_MUTE,
                    category = CATEGORY_VOLUME,
                    minApi = Build.VERSION_CODES.M,
                    iconRes = R.drawable.ic_volume_mute_black_24dp,
                    descriptionRes = R.string.action_toggle_mute
            ),
            //VOLUME

            //SCREEN ORIENTATION
            SystemActionDef(
                    id = TOGGLE_AUTO_ROTATE,
                    category = CATEGORY_SCREEN_ROTATION,
                    permission = Manifest.permission.WRITE_SETTINGS,
                    iconRes = R.drawable.ic_screen_rotation_black_24dp,
                    descriptionRes = R.string.action_toggle_auto_rotate
            ),
            SystemActionDef(
                    id = ENABLE_AUTO_ROTATE,
                    category = CATEGORY_SCREEN_ROTATION,
                    permission = Manifest.permission.WRITE_SETTINGS,
                    iconRes = R.drawable.ic_screen_rotation_black_24dp,
                    descriptionRes = R.string.action_enable_auto_rotate
            ),
            SystemActionDef(
                    id = DISABLE_AUTO_ROTATE,
                    category = CATEGORY_SCREEN_ROTATION,
                    permission = Manifest.permission.WRITE_SETTINGS,
                    iconRes = R.drawable.ic_screen_lock_rotation_black_24dp,
                    descriptionRes = R.string.action_disable_auto_rotate
            ),
            SystemActionDef(
                    id = PORTRAIT_MODE,
                    category = CATEGORY_SCREEN_ROTATION,
                    permission = Manifest.permission.WRITE_SETTINGS,
                    iconRes = R.drawable.ic_stay_current_portrait_black_24dp,
                    descriptionRes = R.string.action_portrait_mode
            ),
            SystemActionDef(
                    id = LANDSCAPE_MODE,
                    category = CATEGORY_SCREEN_ROTATION,
                    permission = Manifest.permission.WRITE_SETTINGS,
                    iconRes = R.drawable.ic_stay_current_landscape_black_24dp,
                    descriptionRes = R.string.action_landscape_mode
            ),
            //SCREEN ORIENTATION

            //BRIGHTNESS
            SystemActionDef(
                    id = TOGGLE_AUTO_BRIGHTNESS,
                    category = CATEGORY_BRIGHTNESS,
                    iconRes = R.drawable.ic_brightness_auto_black_24dp,
                    descriptionRes = R.string.action_toggle_auto_brightness
            ),
            SystemActionDef(
                    id = ENABLE_AUTO_BRIGHTNESS,
                    category = CATEGORY_BRIGHTNESS,
                    iconRes = R.drawable.ic_brightness_auto_black_24dp,
                    descriptionRes = R.string.action_enable_auto_brightness
            ),
            SystemActionDef(
                    id = DISABLE_AUTO_BRIGHTNESS,
                    category = CATEGORY_BRIGHTNESS,
                    iconRes = R.drawable.ic_disable_brightness_auto_24dp,
                    descriptionRes = R.string.action_disable_auto_brightness
            ),
            SystemActionDef(
                    id = INCREASE_BRIGHTNESS,
                    category = CATEGORY_BRIGHTNESS,
                    iconRes = R.drawable.ic_brightness_high_black_24dp,
                    descriptionRes = R.string.action_increase_brightness
            ),
            SystemActionDef(
                    id = DECREASE_BRIGHTNESS,
                    category = CATEGORY_BRIGHTNESS,
                    iconRes = R.drawable.ic_brightness_low_black_24dp,
                    descriptionRes = R.string.action_decrease_brightness
            ),
            //BRIGHTNESS
            SystemActionDef(
                    id = SCREENSHOT,
                    category = CATEGORY_OTHER,
                    minApi = Build.VERSION_CODES.P,
                    iconRes = R.drawable.ic_screenshot_black_24dp,
                    descriptionRes = R.string.action_screenshot
                    )
    )

    /**
     * Get all the system actions which meet the system's api level.
     */
    fun getSystemActionDefinitions(): List<SystemActionDef> {
        return sequence {
            SYSTEM_ACTION_DEFINITIONS.forEach {
                if (it.minApi <= Build.VERSION.SDK_INT) yield(it)
            }
        }.toList()
    }

    fun getSystemActionDef(id: String): Result<SystemActionDef> {
        val systemActionDef = SYSTEM_ACTION_DEFINITIONS.find { it.id == id }

        return systemActionDef.createResult(ERROR_CODE_SYSTEM_ACTION_NOT_FOUND, id)
    }
}