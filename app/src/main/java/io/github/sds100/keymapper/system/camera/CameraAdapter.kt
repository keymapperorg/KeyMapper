package io.github.sds100.keymapper.system.camera

import io.github.sds100.keymapper.util.Result

/**
 * Created by sds100 on 17/03/2021.
 */
interface CameraAdapter {
    fun hasFlashFacing(lens: CameraLens): Boolean
    fun enableFlashlight(lens: CameraLens): Result<*>
    fun disableFlashlight(lens: CameraLens): Result<*>
    fun toggleFlashlight(lens: CameraLens): Result<*>
    fun isFlashlightOn(lens: CameraLens): Boolean
}
