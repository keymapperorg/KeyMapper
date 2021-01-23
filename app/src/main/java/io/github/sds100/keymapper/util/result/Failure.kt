package io.github.sds100.keymapper.util.result

import android.Manifest
import android.content.Context
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.BuildUtils
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 29/02/2020.
 */

fun Failure.getFullMessage(ctx: Context) = when (this) {
    is PermissionDenied -> PermissionDenied.getMessageForPermission(ctx, permission)
    is AppNotFound -> ctx.str(R.string.error_app_isnt_installed, packageName)
    is AppDisabled -> ctx.str(R.string.error_app_is_disabled)
    is NoCompatibleImeEnabled -> ctx.str(R.string.error_ime_service_disabled)
    is NoCompatibleImeChosen -> ctx.str(R.string.error_ime_must_be_chosen)
    is OptionsNotRequired -> ctx.str(R.string.error_options_not_required)
    is SystemFeatureNotSupported -> ctx.str(R.string.error_feature_not_available, feature)
    is ConstraintNotFound -> ctx.str(R.string.error_constraint_not_found)
    is ExtraNotFound -> ctx.str(R.string.error_extra_not_found, extraId)
    is NoActionData -> ctx.str(R.string.error_no_action_data)
    is SdkVersionTooLow -> ctx.str(R.string.error_sdk_version_too_low, BuildUtils.getSdkVersionName(sdkVersion))
    is SdkVersionTooHigh -> ctx.str(R.string.error_sdk_version_too_high, BuildUtils.getSdkVersionName(sdkVersion))
    is FeatureUnavailable -> ctx.str(R.string.error_feature_not_available, feature)
    is SystemActionNotFound -> ctx.str(R.string.error_system_action_not_found, id)
    is KeyMapperImeNotFound -> ctx.str(R.string.error_key_mapper_ime_not_found)
    is InputMethodNotFound -> ctx.str(R.string.error_ime_not_found, id)
    is OptionLabelNotFound -> ctx.str(R.string.error_cant_find_option_label, id)
    is NoEnabledInputMethods -> ctx.str(R.string.error_no_enabled_imes)
    is GoogleAppNotFound -> ctx.str(R.string.error_google_app_not_installed)
    is FrontFlashNotFound -> ctx.str(R.string.error_front_flash_not_found)
    is BackFlashNotFound -> ctx.str(R.string.error_back_flash_not_found)
    is ImeNotFound -> ctx.str(R.string.error_ime_not_found, id)
    is DownloadFailed -> ctx.str(R.string.error_download_failed)
    is FileNotCached -> ctx.str(R.string.error_file_not_cached)
    is SSLHandshakeError -> ctx.str(R.string.error_ssl_handshake_exception)
    is DeviceNotFound -> ctx.str(R.string.error_device_not_found)
    is GenericFailure -> exception.toString()
    is EmptyJson -> ctx.str(R.string.error_empty_json)
    is FileAccessDenied -> ctx.str(R.string.error_file_access_denied)
    is FailedToSplitString -> ctx.str(R.string.error_failed_to_split_string, string)
    is InvalidNumber -> ctx.str(R.string.error_invalid_number)
    is NumberTooSmall -> ctx.str(R.string.error_number_too_small, min)
    is NumberTooBig -> ctx.str(R.string.error_number_too_big, max)
    is CantBeEmpty -> ctx.str(R.string.error_cant_be_empty)

    else -> throw Exception("Can't find error message for ${this::class.simpleName}")
}

fun Failure.getBriefMessage(ctx: Context) = when (this) {
    is AppNotFound -> ctx.str(R.string.error_app_isnt_installed_brief)

    else -> getFullMessage(ctx)
}

class PermissionDenied(val permission: String) : RecoverableFailure() {
    companion object {
        fun getMessageForPermission(ctx: Context, permission: String): String {
            val resId = when (permission) {
                Manifest.permission.WRITE_SETTINGS -> R.string.error_action_requires_write_settings_permission
                Manifest.permission.CAMERA -> R.string.error_action_requires_camera_permission
                Manifest.permission.BIND_DEVICE_ADMIN -> R.string.error_need_to_enable_device_admin
                Manifest.permission.READ_PHONE_STATE -> R.string.error_action_requires_read_phone_state_permission
                Manifest.permission.ACCESS_NOTIFICATION_POLICY -> R.string.error_action_notification_policy_permission
                Manifest.permission.WRITE_SECURE_SETTINGS -> R.string.error_need_write_secure_settings_permission
                Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE -> R.string.error_denied_notification_listener_service_permission
                Manifest.permission.CALL_PHONE -> R.string.error_denied_call_phone_permission
                Constants.PERMISSION_ROOT -> R.string.error_requires_root

                else -> throw Exception("Couldn't find permission description for $permission")
            }

            return ctx.str(resId)
        }
    }
}

class AppNotFound(val packageName: String) : RecoverableFailure()
class AppDisabled(val packageName: String) : RecoverableFailure()
class NoCompatibleImeEnabled : RecoverableFailure()
class NoCompatibleImeChosen : RecoverableFailure()
class FileAccessDenied : Failure()
class GenericFailure(val exception: Exception) : Failure()
class EmptyJson : Failure()
class OptionsNotRequired : Failure()
class SystemFeatureNotSupported(val feature: String) : Failure()
class ConstraintNotFound : Failure()
class ExtraNotFound(val extraId: String) : Failure()
class NoActionData : Failure()
class SdkVersionTooLow(val sdkVersion: Int) : Failure()
class SdkVersionTooHigh(val sdkVersion: Int) : Failure()
class FeatureUnavailable(val feature: String) : Failure()
class SystemActionNotFound(val id: String) : Failure()
class KeyMapperImeNotFound : Failure()
class InputMethodNotFound(val id: String) : Failure()
class OptionLabelNotFound(val id: String) : Failure()
class NoEnabledInputMethods : Failure()
class GoogleAppNotFound : RecoverableFailure()
class FrontFlashNotFound : Failure()
class BackFlashNotFound : Failure()
class ImeNotFound(val id: String) : Failure()
class DownloadFailed : Failure()
class FileNotCached : Failure()
class SSLHandshakeError : Failure()
class DeviceNotFound : Failure()
class FailedToSplitString(val string: String) : Failure()
class InvalidNumber : Failure()
class NumberTooBig(val max: Int) : Failure()
class NumberTooSmall(val min: Int) : Failure()
class CantBeEmpty : Failure()
class NoIncompatibleKeyboardsInstalled : Failure()
class NoMediaSessions : Failure()
class IncompatibleBackup : Failure()