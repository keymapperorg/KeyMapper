package io.github.sds100.keymapper.Utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
    fun getPermissionDescriptionRes(permission: String): Int {
        return when (permission) {
            Manifest.permission.WRITE_SETTINGS -> R.string.error_action_requires_write_settings_permission
            Manifest.permission.CAMERA -> R.string.error_action_requires_camera_permission
            Constants.PERMISSION_ROOT -> R.string.error_action_requires_root
            else -> throw Exception("Couldn't find permission description for $permission")
        }
    }
}

fun Context.isPermissionGranted(permission: String): Boolean {
    //a different method must be used for WRITE_SETTINGS permission
    if (permission.contains(Manifest.permission.WRITE_SETTINGS) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return Settings.System.canWrite(this)

    } else if (permission.contains(Constants.PERMISSION_ROOT)) {
        return RootUtils.checkAppHasRootPermission()
    }

    return ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
}

val Context.haveWriteSettingsPermission: Boolean
    get() = this.isPermissionGranted(Manifest.permission.WRITE_SETTINGS)

fun Context.requestPermission(vararg permission: String, requestCode: Int = 0) {
    //WRITE_SETTINGS permission only has to be granted on Marshmallow or higher
    if (permission.contains(Manifest.permission.WRITE_SETTINGS) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

        //open settings to grant permission{
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:${Constants.PACKAGE_NAME}")
        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

        startActivity(intent)

    } else if (permission.contains(Constants.PERMISSION_ROOT)) {
        RootUtils.promptForRootPermission(this)

    }
}