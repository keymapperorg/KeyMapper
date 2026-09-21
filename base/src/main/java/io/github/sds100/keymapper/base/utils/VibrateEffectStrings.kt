package io.github.sds100.keymapper.base.utils

import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.vibration.VibrateEffect

object VibrateEffectStrings {
    fun getLabel(type: VibrateEffect.PredefinedType) = when (type) {
        VibrateEffect.PredefinedType.CLICK -> R.string.vibration_effect_click
        VibrateEffect.PredefinedType.DOUBLE_CLICK -> R.string.vibration_effect_double_click
        VibrateEffect.PredefinedType.HEAVY_CLICK -> R.string.vibration_effect_heavy_click
        VibrateEffect.PredefinedType.TICK -> R.string.vibration_effect_tick
    }
}
