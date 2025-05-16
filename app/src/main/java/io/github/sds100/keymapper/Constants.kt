package io.github.sds100.keymapper

import android.os.Build

object Constants {
    const val MIN_API = Build.VERSION_CODES.LOLLIPOP
    const val MAX_API = 1000
    const val PACKAGE_NAME = BuildConfig.APPLICATION_ID
    const val VERSION = BuildConfig.VERSION_NAME
    const val VERSION_CODE = BuildConfig.VERSION_CODE
    const val MIN_API_FLOATING_BUTTONS = Build.VERSION_CODES.R
}
