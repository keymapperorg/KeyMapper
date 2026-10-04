package io.github.sds100.keymapper.system.display

import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.PhysicalOrientation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DisplayAdapter {
    val isScreenOn: Flow<Boolean>

    val physicalOrientation: Flow<PhysicalOrientation>
    val cachedPhysicalOrientation: PhysicalOrientation
    val isAmbientDisplayEnabled: Flow<Boolean>

    /**
     * The display id the last Key Mapper activity is attached to.
     */
    val activityDisplayId: Int?

    val displays: StateFlow<List<DisplayInfo>>

    fun getDisplay(id: Int): DisplayInfo?

    fun isAutoRotateEnabled(): Boolean
    fun enableAutoRotate(): KMResult<*>
    fun disableAutoRotate(): KMResult<*>
    fun setOrientation(orientation: Orientation): KMResult<*>

    fun isAutoBrightnessEnabled(): Boolean
    fun increaseBrightness(stepPercent: Float, display: Int): KMResult<*>
    fun decreaseBrightness(stepPercent: Float, display: Int): KMResult<*>
    fun enableAutoBrightness(): KMResult<*>
    fun disableAutoBrightness(): KMResult<*>
}
