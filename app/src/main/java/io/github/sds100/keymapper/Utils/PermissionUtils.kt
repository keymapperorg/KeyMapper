package io.github.sds100.keymapper.Utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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
        return ContextCompat.checkSelfPermission(ctx, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(activity: Activity, vararg permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permission, requestCode)
    }

    fun requestWriteSettingsPermission(layout: CoordinatorLayout) {
        Snackbar.make(layout,
                R.string.error_action_requires_write_settings_permission,
                Snackbar.LENGTH_INDEFINITE).apply {

            setAction(R.string.open) {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                } else {
                    TODO("VERSION.SDK_INT < M")
                }
            }

            show()
        }
    }
}