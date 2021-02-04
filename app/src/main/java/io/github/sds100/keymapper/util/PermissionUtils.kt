package io.github.sds100.keymapper.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.hasRootPermission
import io.github.sds100.keymapper.globalPreferences
import io.github.sds100.keymapper.service.DeviceAdmin
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import splitties.alertdialog.appcompat.*
import splitties.systemservices.devicePolicyManager
import splitties.systemservices.notificationManager
import splitties.toast.toast

/**
 * Created by sds100 on 02/04/2020.
 */

object PermissionUtils {

    fun requestStandardPermission(launcher: ActivityResultLauncher<String>, permission: String) {
        launcher.launch(permission)
    }

    fun requestDeviceAdmin(ctx: Context, launcher: ActivityResultLauncher<Intent>) {
        ctx.alertDialog {

            messageResource = R.string.enable_device_admin_message

            okButton {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)

                intent.putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    ComponentName(ctx, DeviceAdmin::class.java))

                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    ctx.str(R.string.error_need_to_enable_device_admin))

                launcher.launch(intent)
            }

            cancelButton()

            show()
        }
    }

    @SuppressLint("InlinedApi")
    fun requestNotificationListenerAccess(launcher: ActivityResultLauncher<Intent>) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            try {
                launcher.launch(this)
            } catch (e: Exception) {
                toast(R.string.error_cant_find_notification_listener_settings)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun requestAccessNotificationPolicy(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            toast(R.string.error_cant_find_dnd_access_settings)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun requestWriteSettings(ctx: Context) {
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${Constants.PACKAGE_NAME}")
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY

            try {
                ctx.startActivity(this)
            } catch (e: Exception) {
                toast(R.string.error_cant_find_write_settings_page)
            }
        }
    }

    fun requestRootPermission(ctx: Context, navController: NavController) {
        ctx.alertDialog {
            titleResource = R.string.dialog_title_root_prompt
            messageResource = R.string.dialog_message_root_prompt
            setIcon(R.drawable.ic_baseline_warning_24)

            okButton {
                navController.navigate(R.id.action_global_settingsFragment)
                Shell.run("su")
            }

            cancelButton()

            show()
        }
    }

    fun requestWriteSecureSettingsPermission(ctx: Context, navController: NavController) {
        ctx.alertDialog {
            messageResource = R.string.dialog_message_write_secure_settings

            positiveButton(R.string.pos_grant_write_secure_settings_guide) {
                UrlUtils.launchCustomTab(
                    ctx,
                    ctx.str(R.string.url_grant_write_secure_settings_guide)
                )
            }

            negativeButton(R.string.pos_enable_root_features) {
                navController.navigate(R.id.action_global_settingsFragment)
            }

            show()
        }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    fun isPermissionGranted(ctx: Context, permission: String): Boolean {
        val hasRootPermission = ctx.globalPreferences.hasRootPermission.firstBlocking()

        when {
            permission == Manifest.permission.WRITE_SETTINGS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                return Settings.System.canWrite(ctx)

            permission == Constants.PERMISSION_ROOT ->
                return hasRootPermission

            permission == Manifest.permission.BIND_DEVICE_ADMIN -> {
                return devicePolicyManager?.isAdminActive(ComponentName(ctx, DeviceAdmin::class.java)) == true
            }

            permission == Manifest.permission.ACCESS_NOTIFICATION_POLICY ->
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    notificationManager.isNotificationPolicyAccessGranted
                } else {
                    true
                }

            permission == Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE -> {
                return NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(Constants.PACKAGE_NAME)
            }

            permission == Manifest.permission.WRITE_SECURE_SETTINGS && hasRootPermission -> {
                RootUtils.executeRootCommand("pm grant ${Constants.PACKAGE_NAME} ${Manifest.permission.WRITE_SECURE_SETTINGS}")
            }
        }

        return ContextCompat.checkSelfPermission(ctx, permission) == PERMISSION_GRANTED
    }

    fun haveWriteSettingsPermission(ctx: Context) =
        isPermissionGranted(ctx, Manifest.permission.WRITE_SETTINGS)

    fun haveWriteSecureSettingsPermission(ctx: Context) =
        isPermissionGranted(ctx, Manifest.permission.WRITE_SECURE_SETTINGS)

    fun canUseShizuku(ctx: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && hasShizukuPermission(ctx)
                && Shizuku.pingBinder()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun hasShizukuPermission(ctx: Context): Boolean {
        return if (Shizuku.isPreV11() && Shizuku.getVersion() < 11) {
            ctx.checkSelfPermission(ShizukuProvider.PERMISSION) == PERMISSION_GRANTED
        } else {
            Shizuku.checkSelfPermission() == PERMISSION_GRANTED
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun requestShizukuPermission(ctx: Context, reqCode: Int, listener: ((requestCode: Int, grantResult: Int) -> Unit)? = null) {
        if (Shizuku.isPreV11() && Shizuku.getVersion() < 11) {
            if (ctx !is Activity) {
                throw IllegalArgumentException("Context must be Activity")
            }

            if (reqCode == -1) {
                throw IllegalArgumentException("Request code can't be -1")
            }

            ctx.requestPermissions(arrayOf(ShizukuProvider.PERMISSION), reqCode)
        } else {
            if (listener != null) {
                Shizuku.addRequestPermissionResultListener(listener)
            }
            Shizuku.requestPermission(reqCode)
        }
    }
}