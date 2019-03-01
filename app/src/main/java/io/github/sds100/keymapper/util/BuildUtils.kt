package io.github.sds100.keymapper.util

import android.os.Build
import android.os.Build.VERSION_CODES.*

/**
 * Created by sds100 on 12/01/2019.
 */

object BuildUtils {
    /**
     * @return the name of the android version for each [Build.VERSION_CODES] after the minimum api for this app.
     *
     * E.g 28 = "Pie 9.0"
     */
    fun getSdkVersionName(version: Int): String {
        return when (version) {
            KITKAT -> "KitKat 4.4"
            LOLLIPOP -> "Lollipop 5.0"
            LOLLIPOP_MR1 -> "Lollipop 5.1"
            M -> "Marshmallow 6.0"
            N -> "Nougat 7.0"
            N_MR1 -> "Nougat 7.1"
            O -> "Oreo 8.0"
            O_MR1 -> "Oreo 8.1"
            P -> "Pie 9.0"
            else -> throw Exception("No name found for this sdk version: $version")
        }
    }
}