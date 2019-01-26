package io.github.sds100.keymapper.Utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.annotation.IntDef
import io.github.sds100.keymapper.ErrorResult
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Services.MyIMEService

/**
 * Created by sds100 on 25/11/2018.
 */

@IntDef(value = [
    ErrorCodeUtils.ERROR_CODE_NO_ACTION_DATA,
    ErrorCodeUtils.ERROR_CODE_ACTION_IS_NULL,
    ErrorCodeUtils.ERROR_CODE_APP_DISABLED,
    ErrorCodeUtils.ERROR_CODE_APP_UNINSTALLED,
    ErrorCodeUtils.ERROR_CODE_PERMISSION_DENIED,
    ErrorCodeUtils.ERROR_CODE_SHORTCUT_NOT_FOUND,
    ErrorCodeUtils.ERROR_CODE_SYSTEM_ACTION_NOT_FOUND,
    ErrorCodeUtils.ERROR_CODE_FEATURE_NOT_AVAILABLE,
    ErrorCodeUtils.ERROR_CODE_SDK_VERSION_TOO_LOW,
    ErrorCodeUtils.ERROR_CODE_ACTION_EXTRA_NOT_FOUND]
)
@Retention(AnnotationRetention.SOURCE)
annotation class ErrorCode

object ErrorCodeUtils {
    const val ERROR_CODE_NO_ACTION_DATA = 0
    const val ERROR_CODE_ACTION_IS_NULL = 1
    const val ERROR_CODE_PERMISSION_DENIED = 2
    const val ERROR_CODE_APP_DISABLED = 3
    const val ERROR_CODE_APP_UNINSTALLED = 4
    const val ERROR_CODE_SHORTCUT_NOT_FOUND = 5
    const val ERROR_CODE_IME_SERVICE_NOT_CHOSEN = 6
    const val ERROR_CODE_SYSTEM_ACTION_NOT_FOUND = 7
    const val ERROR_CODE_SDK_VERSION_TOO_LOW = 8
    const val ERROR_CODE_FEATURE_NOT_AVAILABLE = 9
    const val ERROR_CODE_ACTION_EXTRA_NOT_FOUND = 10

    private val FIXABLE_ERRORS = arrayOf(
            ERROR_CODE_APP_DISABLED,
            ERROR_CODE_APP_UNINSTALLED,
            ERROR_CODE_PERMISSION_DENIED,
            ERROR_CODE_SHORTCUT_NOT_FOUND,
            ERROR_CODE_IME_SERVICE_NOT_CHOSEN
    )

    /**
     * Attempts to fix a given [errorResult].
     *
     * [ERROR_CODE_PERMISSION_DENIED] must be fixed by requesting for a permission in an activity.
     */
    fun fixError(ctx: Context, errorResult: ErrorResult) {
        when (errorResult.errorCode) {
            ERROR_CODE_APP_DISABLED -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${errorResult.data}")
                intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

                ctx.startActivity(intent)
            }

            ERROR_CODE_APP_UNINSTALLED -> {
                PackageUtils.viewAppOnline(ctx, errorResult.data!!)
            }

            ERROR_CODE_SHORTCUT_NOT_FOUND -> {
                PackageUtils.viewAppOnline(ctx, errorResult.data!!)
            }

            ERROR_CODE_IME_SERVICE_NOT_CHOSEN -> {
                val hasRootPermission = RootUtils.checkAppHasRootPermission(ctx)

                if (hasRootPermission) {
                    ImeUtils.switchIme(MyIMEService.getImeId(ctx))
                } else {
                    /* don't send broadcast to OpenIMEPickerBroadcastReceiver because it is only used
                       when outside of the app */
                    val imeManager = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imeManager.showInputMethodPicker()
                }
            }
        }
    }

    fun isErrorFixable(errorCode: Int) = FIXABLE_ERRORS.contains(errorCode)

    /**
     * @return a message describing an error code.
     */
    fun getErrorCodeDescription(ctx: Context, errorResult: ErrorResult): String {
        ctx.apply {
            return when (errorResult.errorCode) {
                ERROR_CODE_ACTION_IS_NULL -> str(R.string.error_must_choose_action)
                ERROR_CODE_NO_ACTION_DATA -> str(R.string.error_must_choose_action)
                ERROR_CODE_APP_DISABLED -> str(R.string.error_app_is_disabled)
                ERROR_CODE_APP_UNINSTALLED -> str(R.string.error_app_isnt_installed)
                ERROR_CODE_SHORTCUT_NOT_FOUND -> str(R.string.error_shortcut_not_found)
                ERROR_CODE_IME_SERVICE_NOT_CHOSEN -> str(R.string.error_ime_must_be_chosen)

                ERROR_CODE_SDK_VERSION_TOO_LOW -> {
                    val versionName = BuildUtils.getSdkVersionName(errorResult.data!!.toInt())
                    str(R.string.error_sdk_version_too_low, versionName)
                }

                ERROR_CODE_FEATURE_NOT_AVAILABLE -> {
                    str(R.string.error_feature_not_available, errorResult.data)
                }

                ERROR_CODE_PERMISSION_DENIED -> {
                    str(PermissionUtils.getPermissionDescriptionRes(errorResult.data!!))
                }

                else -> throw Exception("Can't find a description for this error code: ${errorResult.errorCode}")
            }
        }
    }
}