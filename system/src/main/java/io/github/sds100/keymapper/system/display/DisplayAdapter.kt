package io.github.sds100.keymapper.system.display

import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.PhysicalOrientation
import io.github.sds100.keymapper.common.utils.SizeKM
import kotlinx.coroutines.flow.Flow

interface DisplayAdapter {
    val isScreenOn: Flow<Boolean>
    val orientation: Flow<Orientation>
    val cachedOrientation: Orientation
    val physicalOrientation: Flow<PhysicalOrientation>
    val cachedPhysicalOrientation: PhysicalOrientation
    val size: SizeKM
    val isAmbientDisplayEnabled: Flow<Boolean>

    fun isAutoRotateEnabled(): Boolean
    fun enableAutoRotate(): KMResult<*>
    fun disableAutoRotate(): KMResult<*>
    fun setOrientation(orientation: Orientation): KMResult<*>

    /**
     * Fetch the orientation and bypass the cached value that updates when the listener changes.
     */
    fun fetchOrientation(): Orientation

    fun isAutoBrightnessEnabled(): Boolean
    fun increaseBrightness(): KMResult<*>
    fun decreaseBrightness(): KMResult<*>
    fun enableAutoBrightness(): KMResult<*>
    fun disableAutoBrightness(): KMResult<*>
}
