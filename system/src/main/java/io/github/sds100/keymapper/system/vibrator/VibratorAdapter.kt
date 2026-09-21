package io.github.sds100.keymapper.system.vibrator

interface VibratorAdapter {
    /**
     * Whether [vibrate] with a [PredefinedVibrationEffect] plays a distinct effect on this
     * device rather than falling back to a generic buzz.
     */
    val supportsPredefinedEffects: Boolean

    fun vibrate(duration: Long)
    fun vibrate(effect: PredefinedVibrationEffect)
}
