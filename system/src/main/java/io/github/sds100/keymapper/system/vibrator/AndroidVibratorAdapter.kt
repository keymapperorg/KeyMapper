package io.github.sds100.keymapper.system.vibrator

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidVibratorAdapter @Inject constructor(@ApplicationContext private val context: Context) :
    VibratorAdapter {
    private val vibrator: Vibrator? = context.getSystemService()

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    override val supportsPredefinedEffects: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun vibrate(duration: Long) {
        if (duration <= 0) return

        val effect =
            VibrationEffect.createOneShot(
                duration,
                VibrationEffect.DEFAULT_AMPLITUDE,
            )

        vibrate(effect)
    }

    override fun vibrate(effect: PredefinedVibrationEffect) {
        if (supportsPredefinedEffects) {
            val platformEffect = VibrationEffect.createPredefined(effect.toPlatformEffectId())
            vibrate(platformEffect)
        } else {
            // Predefined effects need API 29. Fall back to a short buzz so the
            // action/trigger still does something on older devices.
            vibrate(FALLBACK_PREDEFINED_EFFECT_DURATION_MS)
        }
    }

    private fun vibrate(effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator?.vibrate(
                effect,
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY),
            )
        } else {
            vibrator?.vibrate(effect)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun PredefinedVibrationEffect.toPlatformEffectId(): Int = when (this) {
        PredefinedVibrationEffect.CLICK -> VibrationEffect.EFFECT_CLICK
        PredefinedVibrationEffect.DOUBLE_CLICK -> VibrationEffect.EFFECT_DOUBLE_CLICK
        PredefinedVibrationEffect.HEAVY_CLICK -> VibrationEffect.EFFECT_HEAVY_CLICK
        PredefinedVibrationEffect.TICK -> VibrationEffect.EFFECT_TICK
    }

    companion object {
        private const val FALLBACK_PREDEFINED_EFFECT_DURATION_MS = 40L
    }
}
