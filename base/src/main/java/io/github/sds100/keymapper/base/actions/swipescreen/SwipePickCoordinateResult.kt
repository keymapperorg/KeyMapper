package io.github.sds100.keymapper.base.actions.swipescreen

import io.github.sds100.keymapper.common.utils.SizeKM
import kotlinx.serialization.Serializable

@Serializable
data class SwipePickCoordinateResult(
    val xStart: Int,
    val yStart: Int,
    val xEnd: Int,
    val yEnd: Int,
    val fingerCount: Int,
    val duration: Int,
    val description: String,
    /**
     * The display size that the coordinates were picked for. See issue #2217.
     */
    val screenResolution: SizeKM? = null,
)
