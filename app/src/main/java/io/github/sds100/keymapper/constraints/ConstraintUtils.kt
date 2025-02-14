package io.github.sds100.keymapper.constraints

/**
 * Created by sds100 on 05/04/2021.
 */
object ConstraintUtils {
    private val COMMON_SUPPORTED_CONSTRAINTS = listOf(
        ConstraintId.APP_IN_FOREGROUND,
        ConstraintId.APP_NOT_IN_FOREGROUND,
        ConstraintId.APP_PLAYING_MEDIA,
        ConstraintId.APP_NOT_PLAYING_MEDIA,
        ConstraintId.MEDIA_PLAYING,
        ConstraintId.MEDIA_NOT_PLAYING,
        ConstraintId.BT_DEVICE_CONNECTED,
        ConstraintId.BT_DEVICE_DISCONNECTED,
        ConstraintId.ORIENTATION_PORTRAIT,
        ConstraintId.ORIENTATION_LANDSCAPE,
        ConstraintId.ORIENTATION_0,
        ConstraintId.ORIENTATION_90,
        ConstraintId.ORIENTATION_180,
        ConstraintId.ORIENTATION_270,
        ConstraintId.FLASHLIGHT_ON,
        ConstraintId.FLASHLIGHT_OFF,
        ConstraintId.WIFI_ON,
        ConstraintId.WIFI_OFF,
        ConstraintId.WIFI_CONNECTED,
        ConstraintId.WIFI_DISCONNECTED,
        ConstraintId.IME_CHOSEN,
        ConstraintId.IME_NOT_CHOSEN,
        ConstraintId.DEVICE_IS_LOCKED,
        ConstraintId.DEVICE_IS_UNLOCKED,
        ConstraintId.IN_PHONE_CALL,
        ConstraintId.NOT_IN_PHONE_CALL,
        ConstraintId.PHONE_RINGING,
        ConstraintId.CHARGING,
        ConstraintId.DISCHARGING,
    )

    val KEY_MAP_ALLOWED_CONSTRAINTS = listOf(
        ConstraintId.SCREEN_ON,
        ConstraintId.SCREEN_OFF,
    ).plus(COMMON_SUPPORTED_CONSTRAINTS)

    val FINGERPRINT_MAP_ALLOWED_CONSTRAINTS = COMMON_SUPPORTED_CONSTRAINTS
}
