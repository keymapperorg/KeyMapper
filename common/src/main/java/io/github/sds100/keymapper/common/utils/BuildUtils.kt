package io.github.sds100.keymapper.common.utils

import android.os.Build

object BuildUtils {
    /**
     * @return the name of the android version for each [Build.VERSION_CODES] after the minimum api for this app.
     *
     * E.g 28 = "Pie 9.0"
     */
    fun getSdkVersionName(version: Int): String =
        when (version) {
            Build.VERSION_CODES.JELLY_BEAN -> "Jelly Bean 4.1"
            Build.VERSION_CODES.JELLY_BEAN_MR1 -> "Jelly Bean 4.2"
            Build.VERSION_CODES.JELLY_BEAN_MR2 -> "Jelly Bean 4.3"
            Build.VERSION_CODES.KITKAT -> "KitKat 4.4"
            Build.VERSION_CODES.LOLLIPOP -> "Lollipop 5.0"
            Build.VERSION_CODES.LOLLIPOP_MR1 -> "Lollipop 5.1"
            Build.VERSION_CODES.M -> "Marshmallow 6.0"
            Build.VERSION_CODES.N -> "Nougat 7.0"
            Build.VERSION_CODES.N_MR1 -> "Nougat 7.1"
            Build.VERSION_CODES.O -> "Oreo 8.0"
            Build.VERSION_CODES.O_MR1 -> "Oreo 8.1"
            Build.VERSION_CODES.P -> "Pie 9.0"
            Build.VERSION_CODES.Q -> "10"
            Build.VERSION_CODES.R -> "11"
            Build.VERSION_CODES.S -> "12"
            Build.VERSION_CODES.S_V2 -> "12L"
            Build.VERSION_CODES.TIRAMISU -> "13"
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> "14"
            Build.VERSION_CODES.VANILLA_ICE_CREAM -> "15"
            else -> "API $version"
        }
}
