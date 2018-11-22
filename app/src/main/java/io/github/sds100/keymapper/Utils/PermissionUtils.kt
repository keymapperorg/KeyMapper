package io.github.sds100.keymapper.Utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 25/10/2018.
 */

object PermissionUtils {

    fun isPermissionGranted(ctx: Context, permission: String): Boolean {
        //a different method must be used for WRITE_SETTINGS permission
        if (permission == Manifest.permission.WRITE_SETTINGS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.System.canWrite(ctx)
        }

        return ContextCompat.checkSelfPermission(ctx, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun hasWriteSettingsPermission(ctx: Context) =
            isPermissionGranted(ctx, Manifest.permission.WRITE_SETTINGS)

    /**
     * If the action requires a permission which the user hasn't granted, a toast message will
     * tell the user they need to grant permission.
     *
     * @return whether a toast was shown.
     */
    fun showPermissionWarningsForAction(ctx: Context, action: Action): Boolean {
        //if the action doesn't require any special permissions, don't bother showing a toast
        val requiredPermission = ActionUtils.getRequiredPermissionForAction(action) ?: return false
        val isPermissionGranted = isPermissionGranted(ctx, requiredPermission)

        //if permission is granted, don't bother showing a toast message
        if (isPermissionGranted) return false

        //show toast message if the action requires WRITE_SETTINGS permission
        if (requiredPermission == Manifest.permission.WRITE_SETTINGS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Toast.makeText(
                    ctx,
                    getPermissionWarningStringRes(requiredPermission),
                    LENGTH_SHORT
            ).show()
        } else {
            //other permissions will go here
        }

        return true
    }

    fun requestPermission(ctx: Context, permission: String, requestCode: Int = 0) {
        if (permission == Manifest.permission.WRITE_SETTINGS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            //open settings to grant permission{
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:${Constants.PACKAGE_NAME}")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

            ctx.startActivity(intent)
        }
    }

    @StringRes
    fun getPermissionWarningStringRes(permission: String): Int {
        return when (permission) {
            Manifest.permission.WRITE_SETTINGS ->
                R.string.error_action_requires_write_settings_permission
            else -> throw Exception("No error message string resource for that permission!")
        }
    }
}