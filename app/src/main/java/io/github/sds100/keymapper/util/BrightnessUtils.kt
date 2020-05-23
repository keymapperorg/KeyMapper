package io.github.sds100.keymapper.util

import android.content.Context
import android.provider.Settings.System.*
import androidx.annotation.IntDef

/**
 * Created by sds100 on 31/10/2018.
 */
object BrightnessUtils {
    @IntDef(value = [
        SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
        SCREEN_BRIGHTNESS_MODE_MANUAL
    ])
    @Retention(AnnotationRetention.SOURCE)
    annotation class BrightnessMode

    /**
     * How much to change the brightness by.
     */
    private const val BRIGHTNESS_CHANGE_STEP = 20

    fun increaseBrightness(ctx: Context) {
        //auto-brightness must be disabled
        setBrightnessMode(ctx, SCREEN_BRIGHTNESS_MODE_MANUAL)

        ctx.getSystemSetting<Int>(SCREEN_BRIGHTNESS)?.let { currentBrightness ->

            var newBrightness = currentBrightness + BRIGHTNESS_CHANGE_STEP

            //the brightness must be between 0 and 255
            if (newBrightness > 255) newBrightness = 255

            ctx.putSystemSetting(SCREEN_BRIGHTNESS, newBrightness)
        }
    }

    fun decreaseBrightness(ctx: Context) {
        //auto-brightness must be disabled
        setBrightnessMode(ctx, SCREEN_BRIGHTNESS_MODE_MANUAL)

        ctx.getSystemSetting<Int>(SCREEN_BRIGHTNESS)?.let { currentBrightness ->

            var newBrightness = currentBrightness - BRIGHTNESS_CHANGE_STEP

            //the brightness must be between 0 and 255
            if (newBrightness < 0) newBrightness = 0

            ctx.putSystemSetting(SCREEN_BRIGHTNESS, newBrightness)
        }
    }

    fun setBrightnessMode(ctx: Context, @BrightnessMode mode: Int) {
        if (!ctx.haveWriteSettingsPermission) return

        ctx.putSystemSetting(SCREEN_BRIGHTNESS_MODE, mode)
    }

    fun toggleAutoBrightness(ctx: Context) {
        if (!ctx.haveWriteSettingsPermission) return

        val currentBrightnessMode = ctx.getSystemSetting<Int>(SCREEN_BRIGHTNESS_MODE)

        if (currentBrightnessMode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            setBrightnessMode(ctx, SCREEN_BRIGHTNESS_MODE_MANUAL)
        } else {
            setBrightnessMode(ctx, SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
        }
    }
}