package io.github.sds100.keymapper.system.display

import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 17/04/2021.
 */
interface DisplayAdapter {
    val isScreenOn: Flow<Boolean>
    val orientation: Orientation

    fun isAutoRotateEnabled(): Boolean
    fun enableAutoRotate(): Result<*>
    fun disableAutoRotate(): Result<*>
    fun setOrientation(orientation: Orientation): Result<*>

    fun isAutoBrightnessEnabled(): Boolean
    fun increaseBrightness(): Result<*>
    fun decreaseBrightness(): Result<*>
    fun enableAutoBrightness(): Result<*>
    fun disableAutoBrightness(): Result<*>
}
