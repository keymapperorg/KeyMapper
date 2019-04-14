package io.github.sds100.keymapper.util

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.DeviceAdmin
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.activity.ConfigKeymapActivity
import org.jetbrains.anko.toast

/**
 * Created by sds100 on 25/10/2018.
 */

object PermissionUtils {
    const val REQUEST_CODE_PERMISSION = 344

    /**
     * @return a string resource describing the [permission]
     */
    @StringRes
    fun getPermissionDescriptionRes(permission: String): Int {
        return when (permission) {
            Manifest.permission.WRITE_SETTINGS -> R.string.error_action_requires_write_settings_permission
            Manifest.permission.CAMERA -> R.string.error_action_requires_camera_permission
            Manifest.permission.BIND_DEVICE_ADMIN -> R.string.error_need_to_enable_device_admin
            Manifest.permission.READ_PHONE_STATE -> R.string.error_action_requires_read_phone_state_permission
            Manifest.permission.ACCESS_NOTIFICATION_POLICY -> R.string.error_action_notification_policy_permission
            Constants.PERMISSION_ROOT -> R.string.error_action_requires_root
            else -> throw Exception("Couldn't find permission description for $permission")
        }
    }

    fun requestPermission(activity: Activity, permission: String) {
        activity.apply {
            //WRITE_SETTINGS permission only has to be granted on Marshmallow or higher
            if (permission == Manifest.permission.WRITE_SETTINGS &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                //open settings to grant permission{
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:${Constants.PACKAGE_NAME}")
                intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    toast(R.string.error_cant_find_write_settings_page)
                }

            } else if (permission == Constants.PERMISSION_ROOT) {
                RootUtils.promptForRootPermission(this)

            } else if (permission == Manifest.permission.BIND_DEVICE_ADMIN) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)

                intent.putExtra(
                        DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        ComponentName(this, DeviceAdmin::class.java))

                intent.putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        str(R.string.error_need_to_enable_device_admin))

                startActivityForResult(intent, ConfigKeymapActivity.REQUEST_CODE_DEVICE_ADMIN)

            } else if (permission == Manifest.permission.ACCESS_NOTIFICATION_POLICY
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivityForResult(intent, REQUEST_CODE_PERMISSION)

            } else {
                ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_CODE_PERMISSION)
            }
        }
    }
}

fun Context.isPermissionGranted(permission: String): Boolean {
    if (permission == Manifest.permission.WRITE_SETTINGS &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return Settings.System.canWrite(this)

    } else if (permission == Constants.PERMISSION_ROOT) {
        return RootUtils.checkAppHasRootPermission(this)

    } else if (permission == Manifest.permission.BIND_DEVICE_ADMIN) {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(ComponentName(this, DeviceAdmin::class.java))

    } else if (permission == Manifest.permission.ACCESS_NOTIFICATION_POLICY) {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.isNotificationPolicyAccessGranted

        } else {
            true
        }
    }

    return ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
}

val Context.accessNotificationPolicy: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        isPermissionGranted(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
    } else {
        true
    }

val Context.haveWriteSettingsPermission: Boolean
    get() = this.isPermissionGranted(Manifest.permission.WRITE_SETTINGS)