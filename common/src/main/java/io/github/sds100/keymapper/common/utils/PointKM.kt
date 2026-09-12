package io.github.sds100.keymapper.common.utils

import kotlinx.serialization.Serializable

/**
 * A Key Mapper point class that is serializable and can be used in unit tests, unlike
 * android.graphics.Point.
 */
@Serializable
data class PointKM(val x: Int, val y: Int)
