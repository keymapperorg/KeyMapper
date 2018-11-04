package io.github.sds100.keymapper.Utils

import android.content.Context
import android.provider.Settings
import android.provider.Settings.System.*
import androidx.annotation.IntDef

/**
 * Created by sds100 on 31/10/2018.
 */
object BrightnessUtils {
    @IntDef(value = [
        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
    ])
    @Retention(AnnotationRetention.SOURCE)
    annotation class BrightnessMode

    fun setBrightnessMode(ctx: Context, @BrightnessMode mode: Int) {
        if (!PermissionUtils.hasWriteSettingsPermission(ctx)) return

        Settings.System.putInt(ctx.contentResolver, SCREEN_BRIGHTNESS_MODE, mode)
    }

    fun toggleAutoBrightness(ctx: Context) {
        if (!PermissionUtils.hasWriteSettingsPermission(ctx)) return

        val currentBrightnessMode =
                Settings.System.getInt(ctx.contentResolver, SCREEN_BRIGHTNESS_MODE)

        if (currentBrightnessMode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            setBrightnessMode(ctx, SCREEN_BRIGHTNESS_MODE_MANUAL)
        } else {
            setBrightnessMode(ctx, SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
        }
    }
}