package io.github.sds100.keymapper.constraints

/**
 * Created by sds100 on 05/04/2021.
 */
object ConstraintUtils {
    private val COMMON_SUPPORTED_CONSTRAINTS = arrayOf(
        ChooseConstraintType.APP_IN_FOREGROUND,
        ChooseConstraintType.APP_NOT_IN_FOREGROUND,
        ChooseConstraintType.APP_PLAYING_MEDIA,
        ChooseConstraintType.BT_DEVICE_CONNECTED,
        ChooseConstraintType.BT_DEVICE_DISCONNECTED,
        ChooseConstraintType.ORIENTATION_PORTRAIT,
        ChooseConstraintType.ORIENTATION_LANDSCAPE,
        ChooseConstraintType.ORIENTATION_0,
        ChooseConstraintType.ORIENTATION_90,
        ChooseConstraintType.ORIENTATION_180,
        ChooseConstraintType.ORIENTATION_270,
    )

    val KEY_MAP_ALLOWED_CONSTRAINTS = arrayOf(
        ChooseConstraintType.SCREEN_ON,
        ChooseConstraintType.SCREEN_OFF
    ).plus(COMMON_SUPPORTED_CONSTRAINTS)

    val FINGERPRINT_MAP_ALLOWED_CONSTRAINTS = COMMON_SUPPORTED_CONSTRAINTS
}