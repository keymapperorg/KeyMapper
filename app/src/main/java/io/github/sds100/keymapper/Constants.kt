package io.github.sds100.keymapper

import android.os.Build

/**
 * Created by sds100 on 22/11/2018.
 */
object Constants {
    const val MIN_API = Build.VERSION_CODES.JELLY_BEAN_MR1
    const val MAX_API = Build.VERSION_CODES.Q
    const val PACKAGE_NAME = BuildConfig.APPLICATION_ID
    const val PERMISSION_ROOT = "io.github.sds100.keymapper.ROOT"
    const val VERSION = BuildConfig.VERSION_NAME
    const val VERSION_CODE = BuildConfig.VERSION_CODE

    const val INPUT_METHOD_ACTION_INPUT_DOWN_UP = "io.github.sds100.keymapper.inputmethod.INPUT_DOWN_UP"
    const val INPUT_METHOD_EXTRA_KEYCODE = "io.github.sds100.keymapper.inputmethod.KEYCODE"
}