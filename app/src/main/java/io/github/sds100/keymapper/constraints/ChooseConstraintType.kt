package io.github.sds100.keymapper.constraints

import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 05/04/2021.
 */

@Serializable
enum class ChooseConstraintType {
    APP_IN_FOREGROUND,
    APP_NOT_IN_FOREGROUND,
    SCREEN_ELEMENT_VISIBLE,
    SCREEN_ELEMENT_NOT_VISIBLE,
    APP_PLAYING_MEDIA,
    APP_NOT_PLAYING_MEDIA,
    MEDIA_NOT_PLAYING,
    MEDIA_PLAYING,

    BT_DEVICE_CONNECTED,
    BT_DEVICE_DISCONNECTED,

    SCREEN_ON,
    SCREEN_OFF,

    ORIENTATION_PORTRAIT,
    ORIENTATION_LANDSCAPE,
    ORIENTATION_0,
    ORIENTATION_90,
    ORIENTATION_180,
    ORIENTATION_270,

    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,

    WIFI_ON,
    WIFI_OFF,
    WIFI_CONNECTED,
    WIFI_DISCONNECTED,

    IME_CHOSEN,
    IME_NOT_CHOSEN,

    DEVICE_IS_LOCKED,
    DEVICE_IS_UNLOCKED,

    IN_PHONE_CALL,
    NOT_IN_PHONE_CALL,
    PHONE_RINGING,

    CHARGING,
    DISCHARGING
}