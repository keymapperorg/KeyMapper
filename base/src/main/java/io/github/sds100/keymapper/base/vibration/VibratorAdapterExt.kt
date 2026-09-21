package io.github.sds100.keymapper.base.vibration

import io.github.sds100.keymapper.system.vibrator.PredefinedVibrationEffect
import io.github.sds100.keymapper.system.vibrator.VibratorAdapter

/**
 * Fires [effect] on the vibrator, falling back to [defaultDurationMs] when [effect] is null.
 */
fun VibratorAdapter.vibrate(effect: VibrateEffect?, defaultDurationMs: Long) {
    when (effect) {
        null -> vibrate(defaultDurationMs)
        else -> vibrate(effect)
    }
}

fun VibratorAdapter.vibrate(effect: VibrateEffect) {
    when (effect) {
        is VibrateEffect.CustomDuration -> vibrate(effect.durationMs)
        is VibrateEffect.Predefined -> vibrate(effect.predefinedType.toSystemEffect())
    }
}

private fun VibrateEffect.PredefinedType.toSystemEffect(): PredefinedVibrationEffect = when (this) {
    VibrateEffect.PredefinedType.CLICK -> PredefinedVibrationEffect.CLICK
    VibrateEffect.PredefinedType.DOUBLE_CLICK -> PredefinedVibrationEffect.DOUBLE_CLICK
    VibrateEffect.PredefinedType.HEAVY_CLICK -> PredefinedVibrationEffect.HEAVY_CLICK
    VibrateEffect.PredefinedType.TICK -> PredefinedVibrationEffect.TICK
}
