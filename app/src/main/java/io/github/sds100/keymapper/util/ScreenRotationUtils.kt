package io.github.sds100.keymapper.util

import android.content.Context
import android.provider.Settings
import android.view.Surface

/**
 * Created by sds100 on 24/10/2018.
 */
object ScreenRotationUtils {

    fun forcePortraitMode(ctx: Context) {
        if (!ctx.haveWriteSettingsPermission) return

        //auto rotate must be disabled for this to work
        disableAutoRotate(ctx)
        ctx.putSystemSetting(Settings.System.USER_ROTATION, Surface.ROTATION_0)
    }

    fun forceLandscapeMode(ctx: Context) {
        if (!ctx.haveWriteSettingsPermission) return

        //auto rotate must be disabled for this to work
        disableAutoRotate(ctx)
        ctx.putSystemSetting(Settings.System.USER_ROTATION, Surface.ROTATION_90)
    }

    fun switchOrientation(ctx: Context) {

        if (isPortrait(ctx).isNotNullAndTrue()) {
            forceLandscapeMode(ctx)
        } else if (isLandscape(ctx).isNotNullAndTrue()) {
            forcePortraitMode(ctx)
        }
    }

    fun enableAutoRotate(ctx: Context) {
        //don't attempt to enable auto rotate if they app doesn't have permission
        if (!ctx.haveWriteSettingsPermission) return

        ctx.putSystemSetting(Settings.System.ACCELEROMETER_ROTATION, 1)
    }

    fun disableAutoRotate(ctx: Context) {
        //don't attempt to enable auto rotate if they app doesn't have permission
        if (!ctx.haveWriteSettingsPermission) return

        ctx.putSystemSetting(Settings.System.ACCELEROMETER_ROTATION, 0)
    }

    fun toggleAutoRotate(ctx: Context) {

        val autoRotateState = ctx.getSystemSetting<Int>(Settings.System.ACCELEROMETER_ROTATION)

        if (autoRotateState == 0) {
            enableAutoRotate(ctx)
        } else {
            disableAutoRotate(ctx)
        }
    }

    /**
     * @return If the rotation setting can't be found, it returns null
     */
    fun isPortrait(ctx: Context): Boolean? {
        val setting = ctx.getSystemSetting<Int>(Settings.System.USER_ROTATION)
        return setting == Surface.ROTATION_0 || setting == Surface.ROTATION_180
    }

    /**
     * @return If the rotation setting can't be found, it returns null
     */
    fun isLandscape(ctx: Context): Boolean? {
        val setting = ctx.getSystemSetting<Int>(Settings.System.USER_ROTATION)
        return setting == Surface.ROTATION_90 || setting == Surface.ROTATION_270
    }
}