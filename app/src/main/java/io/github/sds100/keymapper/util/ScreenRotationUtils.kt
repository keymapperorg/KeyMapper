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
        setRotation(ctx, Surface.ROTATION_180)
    }

    fun forceLandscapeMode(ctx: Context) {
        if (!ctx.haveWriteSettingsPermission) return

        //auto rotate must be disabled for this to work
        disableAutoRotate(ctx)
        setRotation(ctx, Surface.ROTATION_90)
    }

    fun cycleRotations(ctx: Context, rotations: List<Int>) {
        disableAutoRotate(ctx)

        val currentRotation = getRotation(ctx)

        val index = rotations.indexOf(currentRotation)

        val nextRotation = if (index == rotations.lastIndex) {
            rotations[0]
        } else {
            rotations[index + 1]
        }

        setRotation(ctx, nextRotation)
    }

    fun switchOrientation(ctx: Context) {

        if (isPortrait(ctx)) {
            forceLandscapeMode(ctx)
        } else if (isLandscape(ctx)) {
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
    private fun isPortrait(ctx: Context): Boolean {
        val rotation = ctx.getSystemSetting<Int>(Settings.System.USER_ROTATION)
        return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
    }

    /**
     * @return If the rotation setting can't be found, it returns null
     */
    private fun isLandscape(ctx: Context): Boolean {
        val rotation = getRotation(ctx)
        return rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
    }

    private fun getRotation(ctx: Context) = ctx.getSystemSetting<Int>(Settings.System.USER_ROTATION)
    private fun setRotation(ctx: Context, rotation: Int) = ctx.putSystemSetting(Settings.System.USER_ROTATION, rotation)
}