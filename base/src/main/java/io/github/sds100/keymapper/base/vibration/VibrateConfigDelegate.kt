package io.github.sds100.keymapper.base.vibration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.utils.ui.compose.VibrationEffectMode
import io.github.sds100.keymapper.system.vibrator.VibratorAdapter
import javax.inject.Inject

private const val MAX_PREVIEW_VIBRATION_DURATION_MS = 1000L

data class VibrateConfigState(
    val mode: VibrationEffectMode = VibrationEffectMode.DURATION,
    val durationMs: Int,
    val defaultDurationMs: Int,
    val predefinedType: VibrateEffect.PredefinedType = VibrateEffect.PredefinedType.CLICK,
    val isPredefinedSupported: Boolean = true,
)

/**
 * Shared UI logic for picking a [VibrateEffect] (a custom duration or a predefined haptic
 * effect), used by both the Vibrate action and the key map "vibrate on trigger" option. It plays
 * a short preview vibration whenever the mode, duration or predefined effect changes so the user
 * can feel what they're configuring.
 */
interface VibrateConfigDelegate {
    val vibrateConfigState: VibrateConfigState?

    fun openVibrateConfig(effect: VibrateEffect?, defaultDurationMs: Int)
    fun closeVibrateConfig()
    fun onVibrateModeChange(mode: VibrationEffectMode)
    fun onVibrateDurationChange(durationMs: Int)
    fun onVibratePredefinedTypeChange(type: VibrateEffect.PredefinedType)

    /**
     * @return the [VibrateEffect] built from the current config state, or null if nothing is
     * currently being configured.
     */
    fun buildVibrateEffect(): VibrateEffect?
}

@ViewModelScoped
class VibrateConfigDelegateImpl @Inject constructor(private val vibratorAdapter: VibratorAdapter) :
    VibrateConfigDelegate {

    override var vibrateConfigState: VibrateConfigState? by mutableStateOf(null)
        private set

    override fun openVibrateConfig(effect: VibrateEffect?, defaultDurationMs: Int) {
        val isPredefinedSupported = vibratorAdapter.supportsPredefinedEffects
        val usePredefined = effect is VibrateEffect.Predefined && isPredefinedSupported

        vibrateConfigState = VibrateConfigState(
            mode = if (usePredefined) {
                VibrationEffectMode.PREDEFINED
            } else {
                VibrationEffectMode.DURATION
            },
            durationMs = (effect as? VibrateEffect.CustomDuration)?.durationMs?.toInt()
                ?: defaultDurationMs,
            defaultDurationMs = defaultDurationMs,
            predefinedType = (effect as? VibrateEffect.Predefined)?.predefinedType
                ?: VibrateEffect.PredefinedType.CLICK,
            isPredefinedSupported = isPredefinedSupported,
        )
    }

    override fun closeVibrateConfig() {
        vibrateConfigState = null
    }

    override fun onVibrateModeChange(mode: VibrationEffectMode) {
        val currentState = vibrateConfigState ?: return

        if (mode == VibrationEffectMode.PREDEFINED && !currentState.isPredefinedSupported) {
            return
        }

        vibrateConfigState = currentState.copy(mode = mode)
        playPreview()
    }

    override fun onVibrateDurationChange(durationMs: Int) {
        vibrateConfigState = vibrateConfigState?.copy(durationMs = durationMs) ?: return
        playPreview()
    }

    override fun onVibratePredefinedTypeChange(type: VibrateEffect.PredefinedType) {
        vibrateConfigState = vibrateConfigState?.copy(predefinedType = type) ?: return
        playPreview()
    }

    override fun buildVibrateEffect(): VibrateEffect? {
        val state = vibrateConfigState ?: return null

        return when (state.mode) {
            VibrationEffectMode.DURATION ->
                VibrateEffect.CustomDuration(state.durationMs.toLong())

            VibrationEffectMode.PREDEFINED ->
                VibrateEffect.Predefined(state.predefinedType)
        }
    }

    private fun playPreview() {
        val effect = buildVibrateEffect() ?: return

        // Cap how long the preview buzzes for so a long custom duration doesn't play in full
        // every time the user drags the slider.
        val previewEffect = if (effect is VibrateEffect.CustomDuration) {
            VibrateEffect.CustomDuration(
                effect.durationMs.coerceAtMost(MAX_PREVIEW_VIBRATION_DURATION_MS),
            )
        } else {
            effect
        }

        vibratorAdapter.vibrate(previewEffect)
    }
}
