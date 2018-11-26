package io.github.sds100.keymapper.Utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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

    fun isPermissionGranted(ctx: Context, permission: String): Boolean {
        //a different method must be used for WRITE_SETTINGS permission
        if (permission.contains(Manifest.permission.WRITE_SETTINGS) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.System.canWrite(ctx)

        } else if (permission.contains(Constants.PERMISSION_ROOT)) {
            return RootUtils.checkAppHasRootPermission()
        }

        return ContextCompat.checkSelfPermission(ctx, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun haveWriteSettingsPermission(ctx: Context) = isPermissionGranted(ctx, Manifest.permission.WRITE_SETTINGS)

    fun requestPermission(ctx: Context, vararg permission: String, requestCode: Int = 0) {
        //WRITE_SETTINGS permission only has to be granted on Marshmallow or higher
        if (permission.contains(Manifest.permission.WRITE_SETTINGS) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            //open settings to grant permission{
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:${Constants.PACKAGE_NAME}")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

            ctx.startActivity(intent)

        } else if (permission.contains(Constants.PERMISSION_ROOT)) {
            RootUtils.promptForRootPermission(ctx)
        }
    }

    @StringRes
    fun getPermissionWarningStringRes(permission: String): Int {
        return when (permission) {
            Manifest.permission.WRITE_SETTINGS -> R.string.error_action_requires_write_settings_permission
            Constants.PERMISSION_ROOT -> R.string.error_action_requires_root
            else -> throw Exception("No error message string resource for that permission!")
        }
    }
}