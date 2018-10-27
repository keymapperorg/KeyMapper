package io.github.sds100.keymapper

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringDef

/**
 * Created by sds100 on 04/08/2018.
 */
object SystemAction {

    @SuppressLint("NewApi")
    @StringDef(value = [
        TOGGLE_WIFI,
        ENABLE_WIFI,
        DISABLE_WIFI,

        TOGGLE_BLUETOOTH,
        ENABLE_BLUETOOTH,
        DISABLE_BLUETOOTH,

        TOGGLE_MOBILE_DATA,
        ENABLE_MOBILE_DATA,
        DISABLE_MOBILE_DATA,

        TOGGLE_AUTO_BRIGHTNESS,

        TOGGLE_AUTO_ROTATE,
        ENABLE_AUTO_ROTATE,
        DISABLE_AUTO_ROTATE,
        PORTRAIT_MODE,
        LANDSCAPE_MODE,

        VOLUME_UP,
        VOLUME_DOWN,
        VOLUME_SHOW_DIALOG,
        VOLUME_TOGGLE_MUTE,
        VOLUME_UNMUTE,
        VOLUME_MUTE
    ])
    @Retention(AnnotationRetention.SOURCE)
    annotation class SystemActionId

    //wifi
    const val TOGGLE_WIFI = "toggle_wifi"
    const val ENABLE_WIFI = "enable_wifi"
    const val DISABLE_WIFI = "disable_wifi"

    //bluetooth
    const val TOGGLE_BLUETOOTH = "toggle_bluetooth"
    const val ENABLE_BLUETOOTH = "enable_bluetooth"
    const val DISABLE_BLUETOOTH = "disable_bluetooth"

    //mobile data
    const val TOGGLE_MOBILE_DATA = "toggle_mobile_data"
    const val ENABLE_MOBILE_DATA = "enable_mobile_data"
    const val DISABLE_MOBILE_DATA = "disable_mobile_data"

    //brightness
    const val TOGGLE_AUTO_BRIGHTNESS = "toggle_auto_brightness"

    //auto-rotate
    const val TOGGLE_AUTO_ROTATE = "toggle_auto_rotate"
    const val ENABLE_AUTO_ROTATE = "enable_auto_rotate"
    const val DISABLE_AUTO_ROTATE = "disable_auto_rotate"
    const val PORTRAIT_MODE = "portrait_mode"
    const val LANDSCAPE_MODE = "landscape_mode"

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