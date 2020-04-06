package io.github.sds100.keymapper.util

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.service.DeviceAdmin
import splitties.alertdialog.appcompat.*
import splitties.experimental.ExperimentalSplittiesApi
import splitties.init.appCtx
import splitties.resources.appStr
import splitties.systemservices.devicePolicyManager
import splitties.systemservices.notificationManager
import splitties.toast.toast

/**
 * Created by sds100 on 02/04/2020.
 */

object PermissionUtils {
    fun requestPermission(activity: FragmentActivity, permission: String, onSuccess: () -> Unit) = activity.apply {
        //WRITE_SETTINGS permission only has to be granted on Marshmallow or higher
        when {
            permission == Manifest.permission.WRITE_SETTINGS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {

                //open settings to grant permission{
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:${Constants.PACKAGE_NAME}")
                intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    toast(R.string.error_cant_find_write_settings_page)
                }
            }

            permission == Constants.PERMISSION_ROOT -> RootUtils.promptForRootPermission(this)

            permission == Manifest.permission.BIND_DEVICE_ADMIN -> alertDialog {

                messageResource = R.string.enable_device_admin_message

                okButton {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)

                    intent.putExtra(
                        DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        ComponentName(activity, DeviceAdmin::class.java))

                    intent.putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        appStr(R.string.error_need_to_enable_device_admin))

                    prepareCall(ActivityResultContracts.StartActivityForResult(), activity.activityResultRegistry) {
                        if (it.resultCode == Activity.RESULT_OK) {
                            onSuccess()
                        }
                    }.launch(intent)
                }

                cancelButton()

                show()
            }

            permission == Manifest.permission.ACCESS_NOTIFICATION_POLICY
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {

                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

                try {
                    prepareCall(ActivityResultContracts.StartActivityForResult(), activity.activityResultRegistry) {
                        if (it.resultCode == Activity.RESULT_OK) {
                            onSuccess()
                        }
                    }.launch(intent)
                } catch (e: Exception) {
                    toast(R.string.error_cant_find_dnd_access_settings)
                }
            }

            permission == Manifest.permission.WRITE_SECURE_SETTINGS -> {
                requestWriteSecureSettingsPermission(activity)
            }

            else -> {
                prepareCall(ActivityResultContracts.RequestPermission(), activityResultRegistry) {
                    if (it == true) {
                        onSuccess()
                    }
                }.launch(permission)
            }
        }
    }

    fun requestWriteSecureSettingsPermission(activity: FragmentActivity) = activity.alertDialog {
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

            permission == Manifest.permission.WRITE_SECURE_SETTINGS && AppPreferences.hasRootPermission -> {
                RootUtils.executeRootCommand("pm grant ${Constants.PACKAGE_NAME} ${Manifest.permission.WRITE_SECURE_SETTINGS}")
            }
        }

        return ContextCompat.checkSelfPermission(appCtx, permission) == PERMISSION_GRANTED
    }
}