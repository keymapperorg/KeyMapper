package io.github.sds100.keymapper

/**
 * Created by sds100 on 01/08/2018.
 */

object SystemAction {
    //IDs for each action that can be performed. DO NOT CHANGE!!!

    //wifi
    const val ACTION_TOGGLE_WIFI = "toggle_wifi"
    const val ACTION_ENABLE_WIFI = "enable_wifi"
    const val ACTION_DISABLE_WIFI = "disable_wifi"

    //bluetooth
    const val ACTION_TOGGLE_BLUETOOTH = "toggle_bluetooth"
    const val ACTION_ENABLE_BLUETOOTH = "enable_bluetooth"
    const val ACTION_DISABLE_BLUETOOTH = "disable_bluetooth"

    //mobile data
    const val ACTION_TOGGLE_MOBILE_DATA = "toggle_mobile_data"
    const val ACTION_ENABLE_MOBILE_DATA = "toggle_enable_data"
    const val ACTION_DISABLE_MOBILE_DATA = "toggle_disable_data"

    //brightness
    const val ACTION_TOGGLE_AUTO_BRIGHTNESS = "toggle_auto_brightness"

    //auto-rotate
    const val ACTION_TOGGLE_AUTO_ROTATE = "toggle_auto_rotate"
    const val ACTION_ENABLE_AUTO_ROTATE = "enable_auto_rotate"
    const val ACTION_DISABLE_AUTO_ROTATE = "disable_auto_rotate"
    const val ACTION_PORTRAIT_MODE = "portrait_mode"

    const val ACTION_LANDSCAPE_MODE = "landcape_mode"

    val SYSTEM_ACTION_LIST_ITEMS = listOf(
            SystemActionListItem(ACTION_TOGGLE_WIFI, R.string.action_toggle_wifi, R.drawable.ic_network_wifi_black_24dp),
            SystemActionListItem(ACTION_TOGGLE_BLUETOOTH, R.string.action_toggle_bluetooth, R.drawable.ic_bluetooth_black_24dp))

}