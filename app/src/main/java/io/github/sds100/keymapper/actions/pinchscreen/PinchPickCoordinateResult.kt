package io.github.sds100.keymapper.actions.pinchscreen

import kotlinx.serialization.Serializable

enum class PinchScreenType {
    PINCH_IN,
    PINCH_OUT
}

@Serializable
data class PinchPickCoordinateResult(
    val x: Int,
    val y: Int,
    val distance: Int,
    val pinchType: PinchScreenType,
    val fingerCount: Int,
    val duration: Int,
    val description: String
)
