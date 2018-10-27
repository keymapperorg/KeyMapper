package io.github.sds100.keymapper.Utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
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

    fun requestPermission(activity: Activity, vararg permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permission, requestCode)
    }

    /**
     * Prompt the user with a snackbar to grant permission to the app to modify system settings.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun requestWriteSettingsPermission(layout: CoordinatorLayout) {
        Snackbar.make(layout,
                R.string.error_action_requires_write_settings_permission,
                Snackbar.LENGTH_INDEFINITE).apply {

            //open settings to grant permission
            setAction(R.string.open) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:io.github.sds100.keymapper")
                intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

                layout.context.startActivity(intent)
            }

            show()
        }
    }
}