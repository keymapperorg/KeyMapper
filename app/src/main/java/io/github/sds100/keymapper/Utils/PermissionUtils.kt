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
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.Activities.BaseActivity
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

    /**
     * If the action requires a permission which the user hasn't granted, a toast message will
     * tell the user they need to grant permission.
     *
     * @return whether a toast was shown.
     */
    fun showPermissionWarningsForAction(ctx: Context, action: Action): Boolean {
        val requiredPermission = ActionUtils.getRequiredPermission(action) ?: return false

        //show toast message if the action requires WRITE_SETTINGS permission
        if (requiredPermission == Manifest.permission.WRITE_SETTINGS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Toast.makeText(
                    ctx,
                    R.string.error_action_requires_write_settings_permission,
                    LENGTH_SHORT
            ).show()
        } else {
            //other permissions will go here
        }

        return true
    }

    /**
     * If the action requires a permission which the user hasn't granted, then a snackbar will
     * prompt the user to give permission.
     *
     * @return whether a snackbar was shown.
     */
    fun showPermissionWarningsForAction(
            activity: BaseActivity,
            action: Action,
            requestCode: Int
    ): Boolean {
        val requiredPermission = ActionUtils.getRequiredPermission(action) ?: return false

        if (requiredPermission == Manifest.permission.WRITE_SETTINGS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Snackbar.make(activity.layoutForSnackBar,
                    R.string.error_action_requires_write_settings_permission,
                    Snackbar.LENGTH_INDEFINITE).apply {

                //open settings to grant permission
                setAction(R.string.open) {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.data = Uri.parse("package:io.github.sds100.keymapper")
                    intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

                    activity.startActivity(intent)
                }

                show()
            }
        } else {
            //other permissions will go here
        }

        return true
    }
}