package io.github.sds100.keymapper

import android.os.Build

/**
 * Created by sds100 on 22/11/2018.
 */
object Constants {
    const val MIN_API = Build.VERSION_CODES.KITKAT
    const val MAX_API = Build.VERSION_CODES.Q
    const val PACKAGE_NAME = BuildConfig.APPLICATION_ID
    const val PERMISSION_ROOT = "$PACKAGE_NAME.ROOT"
    const val VERSION = BuildConfig.VERSION_NAME
}