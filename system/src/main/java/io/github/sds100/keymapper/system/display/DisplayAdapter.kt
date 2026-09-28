package io.github.sds100.keymapper.system.display

import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.PhysicalOrientation
import io.github.sds100.keymapper.common.utils.SizeKM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DisplayAdapter {
    val isScreenOn: Flow<Boolean>
    val orientation: Flow<Orientation>
    val cachedOrientation: Orientation
    val physicalOrientation: Flow<PhysicalOrientation>
    val cachedPhysicalOrientation: PhysicalOrientation
    val isAmbientDisplayEnabled: Flow<Boolean>

    /**
     * The display id the Key Mapper MainActivity is attached to. It is set to null when it is
     * detached.
     */
    val activityDisplayId: Int?

    // TODO remove
    val size: SizeKM

    /**
     * The distinct resolutions supported by all displays, taken from the
     * display's supported modes. The dimensions are in the display's natural orientation.
     */
    val supportedResolutions: StateFlow<Set<SizeKM>>

    fun getDisplayResolutions(id: Int): Set<SizeKM>

    fun isAutoRotateEnabled(): Boolean
    fun enableAutoRotate(): KMResult<*>
    fun disableAutoRotate(): KMResult<*>
    fun setOrientation(orientation: Orientation): KMResult<*>

    /**
     * Fetch the orientation and bypass the cached value that updates when the listener changes.
     */
    fun fetchOrientation(): Orientation

    fun isAutoBrightnessEnabled(): Boolean
    fun increaseBrightness(stepPercent: Float): KMResult<*>
    fun decreaseBrightness(stepPercent: Float): KMResult<*>
    fun enableAutoBrightness(): KMResult<*>
    fun disableAutoBrightness(): KMResult<*>
}
