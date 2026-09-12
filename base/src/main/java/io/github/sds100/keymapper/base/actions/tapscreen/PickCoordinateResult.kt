package io.github.sds100.keymapper.base.actions.tapscreen

import io.github.sds100.keymapper.common.utils.SizeKM
import kotlinx.serialization.Serializable

@Serializable
data class PickCoordinateResult(
    val x: Int,
    val y: Int,
    val description: String,
    /**
     * The display size that the coordinate was picked for. See issue #2217.
     */
    val screenResolution: SizeKM? = null,
)
