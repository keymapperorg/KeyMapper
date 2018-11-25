package io.github.sds100.keymapper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import io.github.sds100.keymapper.Utils.PackageUtils
import io.github.sds100.keymapper.Utils.PermissionUtils

/**
 * Created by sds100 on 25/11/2018.
 */
object ErrorCodeUtils {

    const val ERROR_CODE_NO_ACTION_DATA = 0
    const val ERROR_CODE_ACTION_IS_NULL = 1
    const val ERROR_CODE_PERMISSION_DENIED = 2
    const val ERROR_CODE_APP_DISABLED = 3
    const val ERROR_CODE_APP_UNINSTALLED = 4
    const val ERROR_CODE_SHORTCUT_NOT_FOUND = 5
    const val ERROR_CODE_IME_SERVICE_NOT_CHOSEN = 6

    fun fixError(ctx: Context, errorCodeResult: ErrorCodeResult) {
        when (errorCodeResult.errorCode) {
            ERROR_CODE_PERMISSION_DENIED -> {
                PermissionUtils.requestPermission(ctx, errorCodeResult.data!!)
            }

            ERROR_CODE_APP_DISABLED -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${errorCodeResult.data}")
                intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

                ctx.startActivity(intent)
            }

            ERROR_CODE_APP_UNINSTALLED -> {
                PackageUtils.viewAppOnline(ctx, errorCodeResult.data!!)
            }

            ERROR_CODE_SHORTCUT_NOT_FOUND -> {
                PackageUtils.viewAppOnline(ctx, errorCodeResult.data!!)
            }

            ERROR_CODE_IME_SERVICE_NOT_CHOSEN -> {
                val intent = Intent(ctx, OpenIMEPickerBroadcastReceiver::class.java).apply {
                    action = OpenIMEPickerBroadcastReceiver.ACTION_SHOW_IME_PICKER
                }

                ctx.sendBroadcast(intent)
            }
        }
    }

    /**
     * @return the string id of the message describing an error code
     */
    fun getErrorCodeResultDescription(ctx: Context, errorCodeResult: ErrorCodeResult): String? {
        return when (errorCodeResult.errorCode) {
            ERROR_CODE_ACTION_IS_NULL -> ctx.getString(R.string.error_must_choose_action)
            ERROR_CODE_NO_ACTION_DATA -> ctx.getString(R.string.error_must_choose_action)
            ERROR_CODE_APP_DISABLED -> ctx.getString(R.string.error_app_is_disabled)
            ERROR_CODE_APP_UNINSTALLED -> ctx.getString(R.string.error_app_isnt_installed)
            ERROR_CODE_SHORTCUT_NOT_FOUND -> ctx.getString(R.string.error_shortcut_not_found)
            ERROR_CODE_IME_SERVICE_NOT_CHOSEN -> ctx.getString(R.string.error_ime_must_be_chosen)
            ERROR_CODE_PERMISSION_DENIED -> {
                val permissionWarningMessage =
                        PermissionUtils.getPermissionWarningStringRes(errorCodeResult.data!!)

                return ctx.getString(permissionWarningMessage)
            }

            else -> null
        }
    }
}