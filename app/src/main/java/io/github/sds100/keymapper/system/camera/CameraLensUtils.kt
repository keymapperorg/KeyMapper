package io.github.sds100.keymapper.system.camera

import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 23/03/2021.
 */
object CameraLensUtils {
    fun getLabel(lens: CameraLens) = when (lens) {
        CameraLens.FRONT -> R.string.lens_front
        CameraLens.BACK -> R.string.lens_back
    }
}
