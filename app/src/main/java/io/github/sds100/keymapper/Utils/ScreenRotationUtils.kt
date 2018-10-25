package io.github.sds100.keymapper.Utils

import android.Manifest
import android.content.Context
import android.provider.Settings

/**
 * Created by sds100 on 24/10/2018.
 */
object ScreenRotationUtils {

    fun enableAutoRotate(ctx: Context) {
        if (!PermissionUtils.isPermissionGranted(ctx, Manifest.permission.WRITE_SETTINGS)) return

        Settings.System.putInt(ctx.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 1)
    }

    fun disableAutoRotate(ctx: Context) {
        if (!PermissionUtils.isPermissionGranted(ctx, Manifest.permission.WRITE_SETTINGS)) return

        Settings.System.putInt(ctx.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 0)
    }

    fun toggleAutoRotate(ctx: Context) {
        val autoRotateState = Settings.System.getInt(ctx.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION)

        if (autoRotateState == 0) {
            disableAutoRotate(ctx)
        } else {
            enableAutoRotate(ctx)
        }
    }
}