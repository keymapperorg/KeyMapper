package io.github.sds100.keymapper.system.vibrator

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidVibratorAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : VibratorAdapter {
    private val vibrator: Vibrator? = context.getSystemService()

    override fun vibrate(duration: Long) {
        if (duration <= 0) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect =
                VibrationEffect.createOneShot(
                    duration,
                    VibrationEffect.DEFAULT_AMPLITUDE,
                )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator?.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY),
                )
            } else {
                vibrator?.vibrate(effect)
            }
        } else {
            vibrator?.vibrate(duration)
        }
    }
}
