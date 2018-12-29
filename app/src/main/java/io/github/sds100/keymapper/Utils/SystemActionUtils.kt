package io.github.sds100.keymapper.Utils

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.SystemAction
import io.github.sds100.keymapper.SystemAction.CATEGORY_BLUETOOTH
import io.github.sds100.keymapper.SystemAction.CATEGORY_MOBILE_DATA
import io.github.sds100.keymapper.SystemAction.CATEGORY_NAVIGATION
import io.github.sds100.keymapper.SystemAction.CATEGORY_SCREEN_ROTATION
import io.github.sds100.keymapper.SystemAction.CATEGORY_VOLUME
import io.github.sds100.keymapper.SystemAction.CATEGORY_WIFI
import io.github.sds100.keymapper.SystemAction.DISABLE_AUTO_ROTATE
import io.github.sds100.keymapper.SystemAction.DISABLE_BLUETOOTH
import io.github.sds100.keymapper.SystemAction.DISABLE_MOBILE_DATA
import io.github.sds100.keymapper.SystemAction.DISABLE_WIFI
import io.github.sds100.keymapper.SystemAction.ENABLE_AUTO_ROTATE
import io.github.sds100.keymapper.SystemAction.ENABLE_BLUETOOTH
import io.github.sds100.keymapper.SystemAction.ENABLE_MOBILE_DATA
import io.github.sds100.keymapper.SystemAction.ENABLE_WIFI
import io.github.sds100.keymapper.SystemAction.GO_BACK
import io.github.sds100.keymapper.SystemAction.LANDSCAPE_MODE
import io.github.sds100.keymapper.SystemAction.PORTRAIT_MODE
import io.github.sds100.keymapper.SystemAction.TOGGLE_AUTO_ROTATE
import io.github.sds100.keymapper.SystemAction.TOGGLE_BLUETOOTH
import io.github.sds100.keymapper.SystemAction.TOGGLE_MOBILE_DATA
import io.github.sds100.keymapper.SystemAction.TOGGLE_WIFI
import io.github.sds100.keymapper.SystemAction.VOLUME_MUTE
import io.github.sds100.keymapper.SystemAction.VOLUME_TOGGLE_MUTE
import io.github.sds100.keymapper.SystemAction.VOLUME_UNMUTE
import io.github.sds100.keymapper.SystemActionDef

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
            CATEGORY_VOLUME to R.string.system_action_cat_volume
    )

    /**
     * A sorted list of system action definitions
     */
    @SuppressLint("NewApi")
    private val SYSTEM_ACTION_DEFINITIONS = listOf(

            //navigation
            SystemActionDef(
                    id = GO_BACK,
                    category = CATEGORY_NAVIGATION,
                    iconRes = R.drawable.ic_arrow_back_black_24dp,
                    descriptionRes = R.string.action_go_back
            ),

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
            )
            //SCREEN ORIENTATION
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

    @Throws(Exception::class)
    fun getSystemActionDef(id: String): SystemActionDef {
        return SYSTEM_ACTION_DEFINITIONS.find { it.id == id }
                ?: throw Exception("Can't find that system action definition. $id")
    }

//    fun getSystemActionListItems(): List<SystemActionListItem> {
//        //must be in the order the items should be shown to the user
//        return sequence {
//            yield(SystemActionListItem(SystemAction.GO_BACK))
//            yield(SystemActionListItem(SystemAction.GO_HOME))
//            yield(SystemActionListItem(SystemAction.OPEN_RECENTS))
//            yield(SystemActionListItem(SystemAction.OPEN_MENU))

//            yield(SystemActionListItem(SystemAction.TOGGLE_AUTO_ROTATE))
//            yield(SystemActionListItem(SystemAction.ENABLE_AUTO_ROTATE))
//            yield(SystemActionListItem(SystemAction.DISABLE_AUTO_ROTATE))
//            yield(SystemActionListItem(SystemAction.PORTRAIT_MODE))
//            yield(SystemActionListItem(SystemAction.LANDSCAPE_MODE))
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                yield(SystemActionListItem(SystemAction.VOLUME_MUTE))
//                yield(SystemActionListItem(SystemAction.VOLUME_UNMUTE))
//                yield(SystemActionListItem(SystemAction.VOLUME_TOGGLE_MUTE))
//            }
//
//            yield(SystemActionListItem(SystemAction.TOGGLE_AUTO_BRIGHTNESS))
//            yield(SystemActionListItem(SystemAction.ENABLE_AUTO_BRIGHTNESS))
//            yield(SystemActionListItem(SystemAction.DISABLE_AUTO_BRIGHTNESS))
//            yield(SystemActionListItem(SystemAction.INCREASE_BRIGHTNESS))
//            yield(SystemActionListItem(SystemAction.DECREASE_BRIGHTNESS))
//
//            yield(SystemActionListItem(SystemAction.EXPAND_NOTIFICATION_DRAWER))
//            yield(SystemActionListItem(SystemAction.EXPAND_QUICK_SETTINGS))
//            yield(SystemActionListItem(SystemAction.COLLAPSE_STATUS_BAR))
//
//            yield(SystemActionListItem(SystemAction.PAUSE_MEDIA))
//            yield(SystemActionListItem(SystemAction.PLAY_MEDIA))
//            yield(SystemActionListItem(SystemAction.PLAY_PAUSE_MEDIA))
//            yield(SystemActionListItem(SystemAction.NEXT_TRACK))
//            yield(SystemActionListItem(SystemAction.PREVIOUS_TRACK))
//        }.toList()
//    }
//
//    /**
//     * Get resource id for a string which describes a specified [SystemAction].
//     */
//    @SuppressLint("NewApi")
//    @StringRes
//    fun getDescription(@SystemAction.SystemActionId systemAction: String): Int {
//        return when (systemAction) {
//            SystemAction.ENABLE_WIFI -> R.string.action_enable_wifi
//            SystemAction.DISABLE_WIFI -> R.string.action_disable_wifi
//            SystemAction.TOGGLE_WIFI -> R.string.action_toggle_wifi
//
//            SystemAction.ENABLE_BLUETOOTH -> R.string.action_enable_bluetooth
//            SystemAction.DISABLE_BLUETOOTH -> R.string.action_disable_bluetooth
//            SystemAction.TOGGLE_BLUETOOTH -> R.string.action_toggle_bluetooth
//
//            SystemAction.VOLUME_UP -> R.string.action_volume_up
//            SystemAction.VOLUME_DOWN -> R.string.action_volume_down
//            SystemAction.VOLUME_SHOW_DIALOG -> R.string.action_volume_show_dialog
//
//            SystemAction.ENABLE_AUTO_ROTATE -> R.string.action_enable_auto_rotate
//            SystemAction.DISABLE_AUTO_ROTATE -> R.string.action_disable_auto_rotate
//            SystemAction.TOGGLE_AUTO_ROTATE -> R.string.action_toggle_auto_rotate
//            SystemAction.PORTRAIT_MODE -> R.string.action_portrait_mode
//            SystemAction.LANDSCAPE_MODE -> R.string.action_landscape_mode
//
//            SystemAction.VOLUME_MUTE -> return R.string.action_volume_mute
//            SystemAction.VOLUME_TOGGLE_MUTE -> return R.string.action_toggle_mute
//            SystemAction.VOLUME_UNMUTE -> return R.string.action_volume_unmute
//
//            SystemAction.TOGGLE_MOBILE_DATA -> return R.string.action_toggle_mobile_data
//            SystemAction.ENABLE_MOBILE_DATA -> return R.string.action_enable_mobile_data
//            SystemAction.DISABLE_MOBILE_DATA -> return R.string.action_disable_mobile_data
//
//            SystemAction.TOGGLE_AUTO_BRIGHTNESS -> return R.string.action_toggle_auto_brightness
//            SystemAction.DISABLE_AUTO_BRIGHTNESS -> return R.string.action_disable_auto_brightness
//            SystemAction.ENABLE_AUTO_BRIGHTNESS -> return R.string.action_enable_auto_brightness
//            SystemAction.INCREASE_BRIGHTNESS -> return R.string.action_increase_brightness
//            SystemAction.DECREASE_BRIGHTNESS -> return R.string.action_decrease_brightness
//
//            SystemAction.EXPAND_NOTIFICATION_DRAWER -> return R.string.action_expand_notification_drawer
//            SystemAction.EXPAND_QUICK_SETTINGS -> return R.string.action_expand_quick_settings
//            SystemAction.COLLAPSE_STATUS_BAR -> return R.string.action_collapse_status_bar
//
//            SystemAction.PAUSE_MEDIA -> return R.string.action_pause_media
//            SystemAction.PLAY_MEDIA -> return R.string.action_play_media
//            SystemAction.PLAY_PAUSE_MEDIA -> return R.string.action_play_pause_media
//            SystemAction.NEXT_TRACK -> return R.string.action_next_track
//            SystemAction.PREVIOUS_TRACK -> return R.string.action_previous_track
//            SystemAction.GO_BACK -> return R.string.action_go_back
//            SystemAction.GO_HOME -> return R.string.action_go_home
//            SystemAction.OPEN_RECENTS -> return R.string.action_open_recents
//            SystemAction.OPEN_MENU -> return R.string.action_open_menu
//
//            else -> throw Exception("Can't find a description for $systemAction")
//        }
//    }
//
//    /**
//     * Get a drawable resource id for a specified [SystemAction]
//     */
//    @SuppressLint("NewApi")
//    @DrawableRes
//    fun getIconResource(@SystemAction.SystemActionId systemAction: String): Int? {
//        return when (systemAction) {
//            SystemAction.TOGGLE_WIFI -> R.drawable.ic_network_wifi_black_24dp
//            SystemAction.ENABLE_WIFI -> R.drawable.ic_network_wifi_black_24dp
//            SystemAction.DISABLE_WIFI -> R.drawable.ic_signal_wifi_off_black_24dp
//
//            SystemAction.TOGGLE_BLUETOOTH -> R.drawable.ic_bluetooth_black_24dp
//            SystemAction.ENABLE_BLUETOOTH -> R.drawable.ic_bluetooth_black_24dp
//            SystemAction.DISABLE_BLUETOOTH -> R.drawable.ic_bluetooth_disabled_black_24dp
//
//            SystemAction.VOLUME_UP -> R.drawable.ic_volume_up_black_24dp
//            SystemAction.VOLUME_DOWN -> R.drawable.ic_volume_down_black_24dp
//            SystemAction.VOLUME_MUTE -> R.drawable.ic_volume_mute_black_24dp
//            SystemAction.VOLUME_TOGGLE_MUTE -> R.drawable.ic_volume_mute_black_24dp
//            SystemAction.VOLUME_UNMUTE -> R.drawable.ic_volume_up_black_24dp
//
//            SystemAction.ENABLE_AUTO_ROTATE -> R.drawable.ic_screen_rotation_black_24dp
//            SystemAction.DISABLE_AUTO_ROTATE -> R.drawable.ic_screen_lock_rotation_black_24dp
//            SystemAction.TOGGLE_AUTO_ROTATE -> R.drawable.ic_screen_rotation_black_24dp
//            SystemAction.PORTRAIT_MODE -> R.drawable.ic_stay_current_portrait_black_24dp
//            SystemAction.LANDSCAPE_MODE -> R.drawable.ic_stay_current_landscape_black_24dp
//
//            SystemAction.TOGGLE_MOBILE_DATA -> R.drawable.ic_signal
//            SystemAction.ENABLE_MOBILE_DATA -> R.drawable.ic_signal
//            SystemAction.DISABLE_MOBILE_DATA -> R.drawable.ic_signal_off
//
//            SystemAction.TOGGLE_AUTO_BRIGHTNESS -> R.drawable.ic_brightness_auto_black_24dp
//            SystemAction.DISABLE_AUTO_BRIGHTNESS -> R.drawable.ic_disable_brightness_auto_24dp
//            SystemAction.ENABLE_AUTO_BRIGHTNESS -> R.drawable.ic_brightness_auto_black_24dp
//            SystemAction.INCREASE_BRIGHTNESS -> R.drawable.ic_brightness_high_black_24dp
//            SystemAction.DECREASE_BRIGHTNESS -> R.drawable.ic_brightness_low_black_24dp
//
//            SystemAction.PAUSE_MEDIA -> R.drawable.ic_pause_black_24dp
//            SystemAction.PLAY_MEDIA -> R.drawable.ic_play_arrow_black_24dp
//            SystemAction.PLAY_PAUSE_MEDIA -> R.drawable.ic_play_pause_24dp
//            SystemAction.NEXT_TRACK -> R.drawable.ic_skip_next_black_24dp
//            SystemAction.PREVIOUS_TRACK -> R.drawable.ic_skip_previous_black_24dp
//
//            SystemAction.GO_BACK -> R.drawable.ic_arrow_back_black_24dp
//            SystemAction.GO_HOME -> R.drawable.ic_home_black_24dp
//            SystemAction.OPEN_RECENTS -> R.drawable.crop_square
//            SystemAction.OPEN_MENU -> R.drawable.ic_more_vert_black_24dp
//
//            else -> null
//        }
//    }
}