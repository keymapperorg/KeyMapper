package io.github.sds100.keymapper.system.camera

import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.Flow

interface CameraAdapter {
    /**
     * @return null if this lens does not have a flash.
     */
    fun getFlashInfo(lens: CameraLens): CameraFlashInfo?

    /**
     * @param strengthPercent is a percentage of the brightness from 0 to 1.0. Null if the default
     * brightness should be used.
     */
    fun enableFlashlight(lens: CameraLens, strengthPercent: Float?): KMResult<*>

    /**
     * @param strengthPercent is a percentage of the brightness from 0 to 1.0. Null if the default
     * brightness should be used.
     */
    fun toggleFlashlight(lens: CameraLens, strengthPercent: Float?): KMResult<*>
    fun disableFlashlight(lens: CameraLens): KMResult<*>
    fun isFlashlightOn(lens: CameraLens): Boolean
    fun isFlashlightOnFlow(lens: CameraLens): Flow<Boolean>

    /**
     * @param percent This is the percentage of the max strength to increase/decrease by. Set it
     * negative to decrease the strength.
     */
    fun changeFlashlightStrength(lens: CameraLens, percent: Float): KMResult<*>
}
