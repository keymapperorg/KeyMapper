package io.github.sds100.keymapper.system.camera

import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 17/03/2021.
 */
interface CameraAdapter {
    /**
     * @return null if this lens does not have a flash.
     */
    fun getFlashInfo(lens: CameraLens): CameraFlashInfo?

    /**
     * @param strength is a percentage of the brightness from 0 to 1.0. Null if the default
     * brightness should be used.
     */
    fun enableFlashlight(lens: CameraLens, strength: Float?): Result<*>

    /**
     * @param strength is a percentage of the brightness from 0 to 1.0. Null if the default
     * brightness should be used.
     */
    fun toggleFlashlight(lens: CameraLens, strength: Float?): Result<*>
    fun disableFlashlight(lens: CameraLens): Result<*>
    fun isFlashlightOn(lens: CameraLens): Boolean
    fun isFlashlightOnFlow(lens: CameraLens): Flow<Boolean>
}
