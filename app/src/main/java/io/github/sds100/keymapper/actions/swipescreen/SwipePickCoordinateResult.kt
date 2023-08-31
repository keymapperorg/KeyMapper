package io.github.sds100.keymapper.actions.swipescreen

import kotlinx.serialization.Serializable

@Serializable
data class SwipePickCoordinateResult(
    val xStart: Int,
    val yStart: Int,
    val xEnd: Int,
    val yEnd: Int,
    val fingerCount: Int,
    val duration: Int,
    val description: String
)
