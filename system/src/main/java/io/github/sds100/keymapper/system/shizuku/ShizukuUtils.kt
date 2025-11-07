package io.github.sds100.keymapper.system.shizuku

import android.os.Build

object ShizukuUtils {
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    /**
     * @return whether it is recommended to use Shizuku on this Android version. It is set to
     * Android 11 because a PC/mac isn't needed after every reboot to make it work.
     */
    fun isRecommendedForSdkVersion(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
}
