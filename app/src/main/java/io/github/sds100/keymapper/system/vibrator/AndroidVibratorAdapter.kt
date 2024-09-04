package io.github.sds100.keymapper.system.vibrator

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService

/**
 * Created by sds100 on 17/04/2021.
 */
class AndroidVibratorAdapter(context: Context) : VibratorAdapter {
    private val vibrator: Vibrator? = context.getSystemService()

    override fun vibrate(duration: Long) {
        if (duration <= 0) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect =
                VibrationEffect.createOneShot(
                    duration,
                    VibrationEffect.DEFAULT_AMPLITUDE,
                )

            vibrator?.vibrate(effect)
        } else {
            vibrator?.vibrate(duration)
        }
    }
}
