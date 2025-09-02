package io.github.sds100.keymapper.common.utils

import kotlinx.serialization.Serializable

/**
 * A Key Mapper size class that is serializable.
 */
@Serializable
data class SizeKM(val width: Int, val height: Int)
