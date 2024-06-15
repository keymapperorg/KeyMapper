package io.github.sds100.keymapper.system

import android.os.Build
import android.os.Build.VERSION_CODES.JELLY_BEAN
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR1
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.LOLLIPOP_MR1
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.N
import android.os.Build.VERSION_CODES.N_MR1
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.O_MR1
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.Q
import android.os.Build.VERSION_CODES.R
import android.os.Build.VERSION_CODES.S
import android.os.Build.VERSION_CODES.S_V2

/**
 * Created by sds100 on 12/01/2019.
 */

object BuildUtils {
    /**
     * @return the name of the android version for each [Build.VERSION_CODES] after the minimum api for this app.
     *
     * E.g 28 = "Pie 9.0"
     */
    fun getSdkVersionName(version: Int): String = when (version) {
        JELLY_BEAN -> "Jelly Bean 4.1"
        JELLY_BEAN_MR1 -> "Jelly Bean 4.2"
        JELLY_BEAN_MR2 -> "Jelly Bean 4.3"
        KITKAT -> "KitKat 4.4"
        LOLLIPOP -> "Lollipop 5.0"
        LOLLIPOP_MR1 -> "Lollipop 5.1"
        M -> "Marshmallow 6.0"
        N -> "Nougat 7.0"
        N_MR1 -> "Nougat 7.1"
        O -> "Oreo 8.0"
        O_MR1 -> "Oreo 8.1"
        P -> "Pie 9.0"
        Q -> "10"
        R -> "11"
        S -> "12"
        S_V2 -> "12L"
        else -> "API $version"
    }
}
