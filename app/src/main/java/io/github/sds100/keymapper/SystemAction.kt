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
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE
}