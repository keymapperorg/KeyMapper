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

        val ratio = aspectRatio
        val otherRatio = other.aspectRatio
        val otherRotatedRatio = other.height.toFloat() / other.width

        return abs(ratio - otherRatio) <= epsilon || abs(ratio - otherRotatedRatio) <= epsilon
    }

    fun rotate(orientation: Orientation): SizeKM {
        return when (orientation) {
            Orientation.ORIENTATION_0, Orientation.ORIENTATION_180 -> SizeKM(width, height)
            Orientation.ORIENTATION_90, Orientation.ORIENTATION_270 -> SizeKM(height, width)
        }
    }
}

val SizeKM.aspectRatio: Float
    get() = width.toFloat() / height
