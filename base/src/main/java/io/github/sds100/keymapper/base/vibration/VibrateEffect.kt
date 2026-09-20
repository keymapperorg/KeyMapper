package io.github.sds100.keymapper.base.vibration

import kotlinx.serialization.Serializable

@Serializable
sealed class VibrateEffect {
    @Serializable
    data class CustomDuration(val durationMs: Long) : VibrateEffect()

    @Serializable
    data class Predefined(val predefinedType: PredefinedType) : VibrateEffect()

    @Serializable
    enum class PredefinedType {
        CLICK,
        DOUBLE_CLICK,
        HEAVY_CLICK,
        TICK,
    }
}
