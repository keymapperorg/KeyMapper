package io.github.sds100.keymapper

import android.os.Build

/**
 * Created by sds100 on 22/11/2018.
 */
object Constants {
    const val MIN_API = Build.VERSION_CODES.LOLLIPOP
    const val MAX_API = 1000
    const val PACKAGE_NAME = BuildConfig.APPLICATION_ID
    const val VERSION = BuildConfig.VERSION_NAME
    const val VERSION_CODE = BuildConfig.VERSION_CODE
}
