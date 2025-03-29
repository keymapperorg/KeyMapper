package io.github.sds100.keymapper.floating

import kotlinx.serialization.Serializable

@Serializable
data class FloatingButtonAppearance(
    val text: String = "",
    val size: Int = DEFAULT_SIZE_DP,
    val borderOpacity: Float = DEFAULT_BORDER_OPACITY,
    val backgroundOpacity: Float = DEFAULT_BACKGROUND_OPACITY,
) {
    companion object {
        const val MIN_SIZE_DP: Int = 20
        const val DEFAULT_SIZE_DP: Int = 40
        const val MAX_SIZE_DP: Int = 120
        const val DEFAULT_BACKGROUND_OPACITY = 0.5f
        const val DEFAULT_BORDER_OPACITY = 1f
    }
}
