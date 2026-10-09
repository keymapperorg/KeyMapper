package io.github.sds100.keymapper.system.display

import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.SizeKM

data class DisplayInfo(
    val id: Int,
    /**
     * The resolution of the current display mode in the natural orientation.
     */
    val physicalSize: SizeKM,
    /**
     * The size in the natural orientation that apps and window coordinates use. This can be
     * smaller than [physicalSize] if the user lowered the screen resolution.
     */
    val logicalSize: SizeKM,
    val rotation: Orientation,
    val supportedSizes: Set<SizeKM>,
)
