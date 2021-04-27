package io.github.sds100.keymapper.constraints

import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 05/04/2021.
 */

@Serializable
enum class ChooseConstraintType {
    APP_IN_FOREGROUND,
    APP_NOT_IN_FOREGROUND,
    APP_PLAYING_MEDIA,

    BT_DEVICE_CONNECTED,
    BT_DEVICE_DISCONNECTED,

    SCREEN_ON,
    SCREEN_OFF,

    ORIENTATION_PORTRAIT,
    ORIENTATION_LANDSCAPE,
    ORIENTATION_0,
    ORIENTATION_90,
    ORIENTATION_180,
    ORIENTATION_270
}