package io.github.sds100.keymapper.Utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 25/10/2018.
 */

object PermissionUtils {

    /**
     * @return a string resource describing the [permission]
     */
    @StringRes
    fun getPermissionDescriptionRes(permission: String): Int {
        return when (permission) {
            Manifest.permission.WRITE_SETTINGS -> R.string.error_action_requires_write_settings_permission
            Manifest.permission.CAMERA -> R.string.error_action_requires_camera_permission
            Constants.PERMISSION_ROOT -> R.string.error_action_requires_root
            else -> throw Exception("Couldn't find permission description for $permission")
        }
    }

    /**
     * @return whether a permission can only be requested from an [Activity].
     */
    fun requiresActivityToRequest(permission: String): Boolean {
        return when (permission) {
            Manifest.permission.CAMERA -> true
            else -> false
        }
    }
}

fun Context.isPermissionGranted(permission: String): Boolean {
    //a different method must be used for WRITE_SETTINGS permission
    if (permission == Manifest.permission.WRITE_SETTINGS &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return Settings.System.canWrite(this)

    } else if (permission == Constants.PERMISSION_ROOT) {
        return RootUtils.checkAppHasRootPermission(this)
    }

    return ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
}

val Context.haveWriteSettingsPermission: Boolean
    get() = this.isPermissionGranted(Manifest.permission.WRITE_SETTINGS)