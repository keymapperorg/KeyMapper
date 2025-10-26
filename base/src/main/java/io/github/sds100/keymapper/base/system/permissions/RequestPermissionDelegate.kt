package io.github.sds100.keymapper.base.system.permissions

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.str
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.system.DeviceAdmin
import io.github.sds100.keymapper.system.notifications.NotificationReceiverAdapterImpl
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import io.github.sds100.keymapper.system.url.UrlUtils
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.negativeButton
import splitties.alertdialog.appcompat.neutralButton
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.positiveButton
import splitties.alertdialog.appcompat.titleResource
import splitties.alertdialog.material.materialAlertDialog

class RequestPermissionDelegate(
    private val activity: AppCompatActivity,
    val showDialogs: Boolean,
    private val permissionAdapter: AndroidPermissionAdapter,
    private val notificationReceiverAdapter: NotificationReceiverAdapterImpl,
    private val buildConfigProvider: BuildConfigProvider,
    private val shizukuAdapter: ShizukuAdapter,
) {

    private val startActivityForResultLauncher =
        activity.activityResultRegistry.register(
            "start_activity",
            activity,
            ActivityResultContracts.StartActivityForResult(),
        ) {
            permissionAdapter.onPermissionsChanged()
        }

    private val requestPermissionLauncher =
        activity.activityResultRegistry.register(
            "request_permission",
            activity,
            ActivityResultContracts.RequestPermission(),
        ) {
            permissionAdapter.onPermissionsChanged()
        }

    fun requestPermission(permission: Permission, navController: NavController?) {
        when (permission) {
            Permission.WRITE_SETTINGS -> requestWriteSettings()
            Permission.CAMERA -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            Permission.DEVICE_ADMIN -> requestDeviceAdmin()
            Permission.READ_PHONE_STATE -> requestPermissionLauncher.launch(
                Manifest.permission.READ_PHONE_STATE,
            )
            Permission.ACCESS_NOTIFICATION_POLICY -> requestAccessNotificationPolicy()
            Permission.WRITE_SECURE_SETTINGS -> requestWriteSecureSettings()
            Permission.NOTIFICATION_LISTENER -> notificationReceiverAdapter.start()
            Permission.CALL_PHONE -> requestPermissionLauncher.launch(
                Manifest.permission.CALL_PHONE,
            )
            Permission.SEND_SMS -> requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            Permission.ANSWER_PHONE_CALL -> requestPermissionLauncher.launch(
                Manifest.permission.ANSWER_PHONE_CALLS,
            )
            Permission.FIND_NEARBY_DEVICES -> requestPermissionLauncher.launch(
                Manifest.permission.BLUETOOTH_CONNECT,
            )
            Permission.ROOT -> requestRootPermission()

            Permission.IGNORE_BATTERY_OPTIMISATION ->
                requestIgnoreBatteryOptimisations()

            Permission.SHIZUKU -> shizukuAdapter.requestPermission()

            Permission.ACCESS_FINE_LOCATION ->
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

            Permission.POST_NOTIFICATIONS -> if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {
                val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                )

                // The system will say you have to show a rationale if the user previously
                // denied the permission. Therefore, the permission dialog will not show and so
                // open the notification settings to turn it on manually.
                if (showRationale) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, buildConfigProvider.packageName)

                        activity.startActivity(this)
                    }
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun requestAccessNotificationPolicy() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_CLEAR_TASK
                or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                // Add this flag so user only has to press back once.
                or Intent.FLAG_ACTIVITY_NO_HISTORY,
        )

        try {
            startActivityForResultLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                R.string.error_cant_find_dnd_access_settings,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun requestWriteSettings() {
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${buildConfigProvider.packageName}")

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    // Add this flag so user only has to press back once.
                    or Intent.FLAG_ACTIVITY_NO_HISTORY,
            )

            try {
                activity.startActivity(this)
            } catch (e: Exception) {
                Toast.makeText(
                    activity,
                    R.string.error_cant_find_write_settings_page,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun requestWriteSecureSettings() {
        if (permissionAdapter.isGranted(Permission.SHIZUKU) ||
            permissionAdapter.isGranted(Permission.ROOT)
        ) {
            permissionAdapter.grant(Manifest.permission.WRITE_SECURE_SETTINGS)

            return
        }

        activity.materialAlertDialog {
            titleResource = R.string.dialog_title_write_secure_settings
            messageResource = R.string.dialog_message_write_secure_settings

            positiveButton(R.string.pos_grant_write_secure_settings_guide) {
                UrlUtils.openUrl(
                    activity,
                    activity.str(R.string.url_grant_write_secure_settings_guide),
                )
            }

            negativeButton(R.string.neg_cancel) {
                it.cancel()
            }

            show()
        }
    }

    private fun requestRootPermission() {
        if (showDialogs) {
            activity.materialAlertDialog {
                titleResource = R.string.dialog_title_root_prompt
                messageResource = R.string.dialog_message_root_prompt
                setIcon(R.drawable.ic_baseline_warning_24)

                okButton()
                negativeButton(R.string.neg_cancel) { it.cancel() }

                show()
            }
        }
    }

    private fun requestDeviceAdmin() {
        activity.materialAlertDialog {
            messageResource = R.string.enable_device_admin_message

            okButton {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)

                intent.putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    ComponentName(activity, DeviceAdmin::class.java),
                )

                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        // Add this flag so user only has to press back once.
                        or Intent.FLAG_ACTIVITY_NO_HISTORY,
                )

                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    activity.str(R.string.error_need_to_enable_device_admin),
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
                        activity.str(R.string.url_dont_kill_my_app),
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
                Uri.parse("package:${buildConfigProvider.packageName}"),
            )

            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                activity,
                R.string.error_battery_optimisation_activity_not_found,
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
