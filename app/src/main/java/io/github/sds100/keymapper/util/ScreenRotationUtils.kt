package io.github.sds100.keymapper.util

import android.content.Context
import android.provider.Settings
import android.view.Surface
import timber.log.Timber

/**
 * Created by sds100 on 24/10/2018.
 */
object ScreenRotationUtils {

    fun forcePortraitMode(ctx: Context) {
        if (!PermissionUtils.haveWriteSettingsPermission(ctx)) return

        //auto rotate must be disabled for this to work
        disableAutoRotate(ctx)
        setRotation(ctx, Surface.ROTATION_180)
    }

    fun forceLandscapeMode(ctx: Context) {
        if (!PermissionUtils.haveWriteSettingsPermission(ctx)) return

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
        if (!PermissionUtils.haveWriteSettingsPermission(ctx)) return

        SettingsUtils.putSystemSetting(ctx, Settings.System.ACCELEROMETER_ROTATION, 1)
    }

    fun disableAutoRotate(ctx: Context) {
        //don't attempt to enable auto rotate if they app doesn't have permission
        if (!PermissionUtils.haveWriteSettingsPermission(ctx)) return

        SettingsUtils.putSystemSetting(ctx, Settings.System.ACCELEROMETER_ROTATION, 0)
    }

    fun toggleAutoRotate(ctx: Context) {

        val autoRotateState = SettingsUtils.getSystemSetting<Int>(
            ctx,
            Settings.System.ACCELEROMETER_ROTATION
        )

        if (autoRotateState == 0) {
            enableAutoRotate(ctx)
        } else {
            disableAutoRotate(ctx)
        }
    }

    private fun isPortrait(ctx: Context): Boolean {
        val rotation = SettingsUtils.getSystemSetting<Int>(ctx, Settings.System.USER_ROTATION)
        return rotation?.let { isPortrait(it) } ?: false
    }

    fun isPortrait(rotation: Int) = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
    fun isLandscape(rotation: Int) = rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90

    /**
     * @return If the rotation setting can't be found, it returns null
     */
    private fun isLandscape(ctx: Context): Boolean {
        val rotation = getRotation(ctx)
        return rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
    }

    fun getRotation(ctx: Context): Int? {
        return SettingsUtils.getSystemSetting<Int>(ctx, Settings.System.USER_ROTATION)
    }

    private fun setRotation(ctx: Context, rotation: Int) =
        SettingsUtils.putSystemSetting(ctx, Settings.System.USER_ROTATION, rotation)
}