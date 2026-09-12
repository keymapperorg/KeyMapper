package io.github.sds100.keymapper.common.utils

import kotlin.math.abs
import kotlinx.serialization.Serializable

/**
 * A Key Mapper size class that is serializable.
 */
@Serializable
data class SizeKM(val width: Int, val height: Int) {

    /**
     * Whether [other] has the same aspect ratio as this size, allowing for [other] to be
     * rotated 90 degrees (e.g. portrait vs landscape).
     */
    fun hasSameAspectRatio(other: SizeKM, epsilon: Float = 0.01f): Boolean {
        if (width == 0 || height == 0 || other.width == 0 || other.height == 0) {
            return false
        }

        val ratio = width.toFloat() / height
        val otherRatio = other.width.toFloat() / other.height
        val otherRotatedRatio = other.height.toFloat() / other.width

        return abs(ratio - otherRatio) <= epsilon || abs(ratio - otherRotatedRatio) <= epsilon
    }
}
