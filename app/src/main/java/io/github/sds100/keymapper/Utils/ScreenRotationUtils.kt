package io.github.sds100.keymapper.Utils

import android.content.Context
import android.provider.Settings

/**
 * Created by sds100 on 24/10/2018.
 */
object ScreenRotationUtils {
    fun forcePortraitMode(ctx: Context) {
        if (!PermissionUtils.haveWriteSettingsPermission(ctx)) return

        //auto rotate must be disabled for this to work
        disableAutoRotate(ctx)
        Settings.System.putInt(ctx.contentResolver, Settings.System.USER_ROTATION, 0)
    }

    fun forceLandscapeMode(ctx: Context) {
        if (!PermissionUtils.haveWriteSettingsPermission(ctx)) return

        //auto rotate must be disabled for this to work
        disableAutoRotate(ctx)
        Settings.System.putInt(ctx.contentResolver, Settings.System.USER_ROTATION, 0)
    }

    fun enableAutoRotate(ctx: Context) {
        //don't attempt to enable auto rotate if they app doesn't have permission
        if (!PermissionUtils.haveWriteSettingsPermission(ctx)) return

        Settings.System.putInt(ctx.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 1)
    }

    fun disableAutoRotate(ctx: Context) {
        //don't attempt to enable auto rotate if they app doesn't have permission
        if (!PermissionUtils.haveWriteSettingsPermission(ctx)) return

        Settings.System.putInt(ctx.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 0)
    }

    fun toggleAutoRotate(ctx: Context) {
        val autoRotateState = Settings.System.getInt(ctx.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION)

        if (autoRotateState == 0) {
            enableAutoRotate(ctx)
        } else {
            disableAutoRotate(ctx)
        }
    }
}