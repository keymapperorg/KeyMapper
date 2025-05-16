package io.github.sds100.keymapper.system.display

import io.github.sds100.keymapper.R


object OrientationUtils {
    fun getLabel(orientation: Orientation) = when (orientation) {
        Orientation.ORIENTATION_0 -> R.string.orientation_0
        Orientation.ORIENTATION_90 -> R.string.orientation_90
        Orientation.ORIENTATION_180 -> R.string.orientation_180
        Orientation.ORIENTATION_270 -> R.string.orientation_270
    }
}
