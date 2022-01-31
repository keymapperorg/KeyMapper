package io.github.sds100.keymapper.system.permissions

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.system.DeviceAdmin
import io.github.sds100.keymapper.system.Shell
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.str
import splitties.alertdialog.appcompat.*
import splitties.alertdialog.material.materialAlertDialog
import splitties.toast.longToast
import splitties.toast.toast

/**
 * Created by sds100 on 13/04/2021.
 */
class RequestPermissionDelegate(
    private val activity: AppCompatActivity,
    val showDialogs: Boolean
) {
    private val startActivityForResultLauncher =
        activity.activityResultRegistry.register(
            "start_activity",
            activity,
            ActivityResultContracts.StartActivityForResult()
        ) {
            ServiceLocator.permissionAdapter(activity).onPermissionsChanged()
        }

    private val requestPermissionLauncher =
        activity.activityResultRegistry.register(
            "request_permission",
            activity,
            ActivityResultContracts.RequestPermission()
        ) {
            ServiceLocator.permissionAdapter(activity).onPermissionsChanged()
        }

    fun requestPermission(permission: Permission, navController: NavController?) {
        when (permission) {
            Permission.WRITE_SETTINGS -> requestWriteSettings()
            Permission.CAMERA -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            Permission.DEVICE_ADMIN -> requestDeviceAdmin()
            Permission.READ_PHONE_STATE -> requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            Permission.ACCESS_NOTIFICATION_POLICY -> requestAccessNotificationPolicy()
            Permission.WRITE_SECURE_SETTINGS -> requestWriteSecureSettings()
            Permission.CALL_PHONE -> requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            Permission.ROOT -> {
                require(navController != null) { "nav controller can't be null!" }
                requestRootPermission(navController)
            }

            Permission.IGNORE_BATTERY_OPTIMISATION ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestIgnoreBatteryOptimisations()
                }
        }
    }

    private fun requestAccessNotificationPolicy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

            try {
                startActivityForResultLauncher.launch(intent)
            } catch (e: Exception) {
                toast(R.string.error_cant_find_dnd_access_settings)
            }
        }
    }

    private fun requestWriteSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${Constants.PACKAGE_NAME}")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY

                try {
                    activity.startActivity(this)
                } catch (e: Exception) {
                    toast(R.string.error_cant_find_write_settings_page)
                }
            }
        }
    }

    private fun requestWriteSecureSettings() {
        activity.materialAlertDialog {
            messageResource = R.string.dialog_message_write_secure_settings

            positiveButton(R.string.pos_grant_write_secure_settings_guide) {
                UrlUtils.openUrl(
                    activity,
                    activity.str(R.string.url_grant_write_secure_settings_guide)
                )
            }

            negativeButton(R.string.pos_enable_root_features) {
                val successful = ServiceLocator.suAdapter(context).requestPermission()

                if (successful) {
                    context.toast(R.string.toast_root_features_turned_on)
                }
            }

            show()
        }
    }

    private fun requestRootPermission(navController: NavController) {

        if (showDialogs) {
            activity.materialAlertDialog {
                titleResource = R.string.dialog_title_root_prompt
                messageResource = R.string.dialog_message_root_prompt
                setIcon(R.drawable.ic_baseline_warning_24)

                okButton {
                    navController.navigate(R.id.action_global_settingsFragment)
                    Shell.run("su")
                }

                negativeButton(R.string.neg_cancel) { it.cancel() }

                show()
            }
        } else {
            navController.navigate(R.id.action_global_settingsFragment)
            Shell.run("su")
        }
    }

    private fun requestDeviceAdmin() {
        activity.materialAlertDialog {

            messageResource = R.string.enable_device_admin_message

            okButton {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)

                intent.putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    ComponentName(activity, DeviceAdmin::class.java)
                )

                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    activity.str(R.string.error_need_to_enable_device_admin)
                )

                startActivityForResultLauncher.launch(intent)
            }

            negativeButton(R.string.neg_cancel) { it.cancel() }

            show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestIgnoreBatteryOptimisations() {
        if (showDialogs) {
            activity.materialAlertDialog {
                titleResource = R.string.dialog_title_disable_battery_optimisation
                messageResource = R.string.dialog_message_disable_battery_optimisation

                positiveButton(R.string.pos_turn_off_stock_battery_optimisation) {
                    showBatteryOptimisationExemptionSystemDialog()
                }

                negativeButton(R.string.neg_cancel) { it.cancel() }

                neutralButton(R.string.neutral_go_to_dont_kill_my_app) {
                    UrlUtils.openUrl(
                        activity,
                        activity.str(R.string.url_dont_kill_my_app)
                    )
                }

                show()
            }
        } else {
            showBatteryOptimisationExemptionSystemDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showBatteryOptimisationExemptionSystemDialog() {
        try {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${Constants.PACKAGE_NAME}")
            )

            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            activity.longToast(R.string.error_battery_optimisation_activity_not_found)
        }
    }
}