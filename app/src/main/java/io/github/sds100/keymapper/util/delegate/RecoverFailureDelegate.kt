package io.github.sds100.keymapper.util.delegate

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.KeyboardUtils
import io.github.sds100.keymapper.util.PackageUtils
import io.github.sds100.keymapper.util.PermissionUtils
import io.github.sds100.keymapper.util.result.*
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 22/10/20.
 */

class RecoverFailureDelegate(
    keyPrefix: String,
    resultRegistry: ActivityResultRegistry,
    lifecycleOwner: LifecycleOwner,
    private val onSuccessfulRecover: () -> Unit
) {

    private val startActivityForResultLauncher =
        resultRegistry.register(
            "$keyPrefix.start_activity",
            lifecycleOwner,
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                onSuccessfulRecover.invoke()
            }
        }

    private val requestPermissionLauncher =
        resultRegistry.register(
            "$keyPrefix.request_permission",
            lifecycleOwner,
            ActivityResultContracts.RequestPermission()
        ) {
            if (it) {
                onSuccessfulRecover.invoke()
            }
        }

    fun recover(ctx: Context, failure: RecoverableFailure, navController: NavController) {
        when (failure) {
            is PermissionDenied -> {
                when (failure.permission) {
                    Manifest.permission.WRITE_SETTINGS ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            PermissionUtils.requestWriteSettings(ctx)
                        }

                    Manifest.permission.CAMERA ->
                        PermissionUtils.requestStandardPermission(
                            requestPermissionLauncher,
                            Manifest.permission.CAMERA
                        )

                    Manifest.permission.BIND_DEVICE_ADMIN ->
                        PermissionUtils.requestDeviceAdmin(ctx, startActivityForResultLauncher)

                    Manifest.permission.READ_PHONE_STATE ->
                        PermissionUtils.requestStandardPermission(
                            requestPermissionLauncher,
                            Manifest.permission.READ_PHONE_STATE
                        )

                    Manifest.permission.ACCESS_NOTIFICATION_POLICY ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            PermissionUtils.requestAccessNotificationPolicy(
                                startActivityForResultLauncher
                            )
                        }

                    Manifest.permission.WRITE_SECURE_SETTINGS ->
                        PermissionUtils.requestWriteSecureSettingsPermission(ctx, navController)

                    Constants.PERMISSION_ROOT -> PermissionUtils.requestRootPermission(
                        ctx,
                        navController
                    )

                    Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE ->
                        PermissionUtils.requestNotificationListenerAccess(
                            startActivityForResultLauncher
                        )

                    Manifest.permission.CALL_PHONE ->
                        PermissionUtils.requestStandardPermission(
                            requestPermissionLauncher,
                            Manifest.permission.CALL_PHONE
                        )

                    else -> throw Exception("Don't know how to ask for permission ${failure.permission}")
                }
            }

            is GoogleAppNotFound -> recover(
                ctx,
                AppNotFound(ctx.str(R.string.google_app_package_name)),
                navController
            )

            is AppNotFound -> PackageUtils.viewAppOnline(ctx, failure.packageName)

            is AppDisabled -> {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${failure.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY

                    ctx.startActivity(this)
                }
            }

            is NoCompatibleImeEnabled -> KeyboardUtils.enableCompatibleInputMethods(ctx)
            is NoCompatibleImeChosen -> KeyboardUtils.chooseCompatibleInputMethod(
                ctx,
                fromForeground = true
            )
        }
    }
}