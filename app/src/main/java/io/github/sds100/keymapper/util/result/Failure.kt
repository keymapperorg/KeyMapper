package io.github.sds100.keymapper.util.result

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.PackageUtils
import splitties.resources.appStr

/**
 * Created by sds100 on 29/02/2020.
 */

class PermissionDenied<T>(permission: String) : RecoverableFailure(getMessageForPermission(permission)) {
    companion object {
        fun getMessageForPermission(permission: String): String {
            val resId = when (permission) {
                Manifest.permission.WRITE_SETTINGS -> R.string.error_action_requires_write_settings_permission
                Manifest.permission.CAMERA -> R.string.error_action_requires_camera_permission
                Manifest.permission.BIND_DEVICE_ADMIN -> R.string.error_need_to_enable_device_admin
                Manifest.permission.READ_PHONE_STATE -> R.string.error_action_requires_read_phone_state_permission
                Manifest.permission.ACCESS_NOTIFICATION_POLICY -> R.string.error_action_notification_policy_permission
                Constants.PERMISSION_ROOT -> R.string.error_action_requires_root

                else -> throw Exception("Couldn't find permission description for $permission")
            }

            return appStr(resId)
        }
    }

    override fun recover(ctx: Context) {

    }
}

class AppNotFound(val packageName: String) : RecoverableFailure(
        fullMessage = appStr(R.string.error_app_isnt_installed, packageName),
        briefMessage = appStr(R.string.error_app_isnt_installed_brief)) {
    override fun recover(ctx: Context) = PackageUtils.viewAppOnline(ctx, packageName)
}

class AppDisabled(val packageName: String) : RecoverableFailure(appStr(R.string.error_app_isnt_installed)) {
    override fun recover(ctx: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

        ctx.startActivity(intent)
    }
}

class NoActionData : Failure(appStr(R.string.error_no_action_data))
class FlagNotFound : Failure(appStr(R.string.error_flag_not_found))