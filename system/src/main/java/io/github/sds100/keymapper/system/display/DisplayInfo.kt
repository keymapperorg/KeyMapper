package io.github.sds100.keymapper.system.display

import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.SizeKM

data class DisplayInfo(
    val id: Int,
    val activeSize: SizeKM,
    val rotation: Orientation,
    val supportedSizes: Set<SizeKM>,
)
