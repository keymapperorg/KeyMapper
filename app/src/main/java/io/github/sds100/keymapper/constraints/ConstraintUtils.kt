package io.github.sds100.keymapper.constraints

/**
 * Created by sds100 on 05/04/2021.
 */
object ConstraintUtils {
    private val COMMON_SUPPORTED_CONSTRAINTS = listOf(
        ChooseConstraintType.APP_IN_FOREGROUND,
        ChooseConstraintType.APP_NOT_IN_FOREGROUND,
        ChooseConstraintType.SCREEN_ELEMENT_VISIBLE,
        ChooseConstraintType.SCREEN_ELEMENT_NOT_VISIBLE,
        ChooseConstraintType.APP_PLAYING_MEDIA,
        ChooseConstraintType.APP_NOT_PLAYING_MEDIA,
        ChooseConstraintType.MEDIA_PLAYING,
        ChooseConstraintType.MEDIA_NOT_PLAYING,
        ChooseConstraintType.BT_DEVICE_CONNECTED,
        ChooseConstraintType.BT_DEVICE_DISCONNECTED,
        ChooseConstraintType.ORIENTATION_PORTRAIT,
        ChooseConstraintType.ORIENTATION_LANDSCAPE,
        ChooseConstraintType.ORIENTATION_0,
        ChooseConstraintType.ORIENTATION_90,
        ChooseConstraintType.ORIENTATION_180,
        ChooseConstraintType.ORIENTATION_270,
        ChooseConstraintType.FLASHLIGHT_ON,
        ChooseConstraintType.FLASHLIGHT_OFF,
        ChooseConstraintType.WIFI_ON,
        ChooseConstraintType.WIFI_OFF,
        ChooseConstraintType.WIFI_CONNECTED,
        ChooseConstraintType.WIFI_DISCONNECTED,
        ChooseConstraintType.IME_CHOSEN,
        ChooseConstraintType.IME_NOT_CHOSEN,
        ChooseConstraintType.DEVICE_IS_LOCKED,
        ChooseConstraintType.DEVICE_IS_UNLOCKED,
        ChooseConstraintType.IN_PHONE_CALL,
        ChooseConstraintType.NOT_IN_PHONE_CALL,
        ChooseConstraintType.PHONE_RINGING,
        ChooseConstraintType.CHARGING,
        ChooseConstraintType.DISCHARGING,
    )

    val KEY_MAP_ALLOWED_CONSTRAINTS = listOf(
        ChooseConstraintType.SCREEN_ON,
        ChooseConstraintType.SCREEN_OFF
    ).plus(COMMON_SUPPORTED_CONSTRAINTS)

    val FINGERPRINT_MAP_ALLOWED_CONSTRAINTS = COMMON_SUPPORTED_CONSTRAINTS
}