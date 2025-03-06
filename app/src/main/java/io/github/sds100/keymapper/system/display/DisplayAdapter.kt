package io.github.sds100.keymapper.system.display

import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.SizeKM
import kotlinx.coroutines.flow.Flow

interface DisplayAdapter {
    val isScreenOn: Flow<Boolean>
    val cachedOrientation: Orientation
    val size: SizeKM

    fun isAutoRotateEnabled(): Boolean
    fun enableAutoRotate(): Result<*>
    fun disableAutoRotate(): Result<*>
    fun setOrientation(orientation: Orientation): Result<*>

    /**
     * Fetch the orientation and bypass the cached value that updates when the listener changes.
     */
    fun fetchOrientation(): Orientation

    fun isAutoBrightnessEnabled(): Boolean
    fun increaseBrightness(): Result<*>
    fun decreaseBrightness(): Result<*>
    fun enableAutoBrightness(): Result<*>
    fun disableAutoBrightness(): Result<*>
}
