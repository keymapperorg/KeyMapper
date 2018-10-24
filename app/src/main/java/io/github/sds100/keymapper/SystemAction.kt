package io.github.sds100.keymapper

/**
 * Created by sds100 on 04/08/2018.
 */
enum class SystemAction {
    //wifi
    TOGGLE_WIFI,
    ENABLE_WIFI,
    DISABLE_WIFI,

    //bluetooth
    TOGGLE_BLUETOOTH,
    ENABLE_BLUETOOTH,
    DISABLE_BLUETOOTH,

    //mobile data
    TOGGLE_MOBILE_DATA,
    ENABLE_MOBILE_DATA,
    DISABLE_MOBILE_DATA,

    //brightness
    TOGGLE_AUTO_BRIGHTNESS,

    //auto-rotate
    TOGGLE_AUTO_ROTATE,
    ENABLE_AUTO_ROTATE,
    DISABLE_AUTO_ROTATE,
    PORTRAIT_MODE,
    LANDSCAPE_MODE,

    //volume
    const val VOLUME_UP = "volume_up"
    const val VOLUME_DOWN = "volume_down"
    const val VOLUME_SHOW_DIALOG = "volume_show_dialog"

    @RequiresApi(Build.VERSION_CODES.M)
    const val VOLUME_UNMUTE = "volume_unmute"

    @RequiresApi(Build.VERSION_CODES.M)
    const val VOLUME_MUTE = "volume_mute"

    @RequiresApi(Build.VERSION_CODES.M)
    const val VOLUME_TOGGLE_MUTE = "volume_toggle_mute"
}