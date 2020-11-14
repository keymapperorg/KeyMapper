package io.github.sds100.keymapper.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.service.DeviceAdmin
import splitties.alertdialog.appcompat.*
import splitties.init.appCtx
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

    fun requestRootPermission(activity: FragmentActivity) {
        activity.alertDialog {
            titleResource = R.string.dialog_title_root_prompt
            messageResource = R.string.dialog_message_root_prompt
            setIcon(R.drawable.ic_baseline_warning_24)

            okButton {
                activity.findNavController(R.id.container).navigate(R.id.action_global_settingsFragment)
                Shell.run("su")
            }

            cancelButton()

            show()
        }
    }

    fun requestWriteSecureSettingsPermission(activity: FragmentActivity) {
        activity.alertDialog {
            messageResource = R.string.dialog_message_write_secure_settings

            positiveButton(R.string.pos_help_page) {
                val direction = NavAppDirections.actionGlobalHelpFragment()
                activity.findNavController(R.id.container).navigate(direction)
            }

            negativeButton(R.string.pos_enable_root_features) {
                activity.findNavController(R.id.container).navigate(R.id.action_global_settingsFragment)
            }

            show()
        }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    fun isPermissionGranted(permission: String): Boolean {
        when {
            permission == Manifest.permission.WRITE_SETTINGS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                return Settings.System.canWrite(appCtx)

            permission == Constants.PERMISSION_ROOT ->
                return AppPreferences.hasRootPermission

            permission == Manifest.permission.BIND_DEVICE_ADMIN -> {
                return devicePolicyManager?.isAdminActive(ComponentName(appCtx, DeviceAdmin::class.java)) == true
            }

            permission == Manifest.permission.ACCESS_NOTIFICATION_POLICY ->
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    notificationManager.isNotificationPolicyAccessGranted
                } else {
                    true
                }

            permission == Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE -> {
                return NotificationManagerCompat.getEnabledListenerPackages(appCtx).contains(Constants.PACKAGE_NAME)
            }

            permission == Manifest.permission.WRITE_SECURE_SETTINGS && AppPreferences.hasRootPermission -> {
                RootUtils.executeRootCommand("pm grant ${Constants.PACKAGE_NAME} ${Manifest.permission.WRITE_SECURE_SETTINGS}")
            }
        }

        return ContextCompat.checkSelfPermission(appCtx, permission) == PERMISSION_GRANTED
    }
}


val Context.haveWriteSettingsPermission: Boolean
    get() = PermissionUtils.isPermissionGranted(Manifest.permission.WRITE_SETTINGS)

val Context.haveWriteSecureSettingsPermission: Boolean
    get() = PermissionUtils.isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)