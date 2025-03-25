package io.github.sds100.keymapper.floating

import kotlinx.serialization.Serializable

@Serializable
data class FloatingButtonAppearance(
    val text: String = "",
    val size: Int = DEFAULT_SIZE_DP,
) {
    companion object {
        const val MIN_SIZE_DP: Int = 20
        const val DEFAULT_SIZE_DP: Int = 40
        const val MAX_SIZE_DP: Int = 120
    }
}
