package io.github.sds100.keymapper.util.result

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.fragment.app.Fragment
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.PermissionUtils.isPermissionGranted
import splitties.experimental.ExperimentalSplittiesApi
import splitties.resources.appStr

/**
 * Created by sds100 on 29/02/2020.
 */

class PermissionDenied(private val mPermission: String) : RecoverableFailure(getMessageForPermission(mPermission)) {
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

    override suspend fun recover(fragment: Fragment) {
        PermissionUtils.requestPermission(fragment, mPermission)
    }
}

class AppNotFound(val packageName: String) : RecoverableFailure(
    fullMessage = appStr(R.string.error_app_isnt_installed, packageName),
    briefMessage = appStr(R.string.error_app_isnt_installed_brief)) {
    override suspend fun recover(fragment: Fragment) = PackageUtils.viewAppOnline(packageName)
}

class AppDisabled(val packageName: String) : RecoverableFailure(appStr(R.string.error_app_isnt_installed)) {
    override suspend fun recover(fragment: Fragment) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

        fragment.startActivity(intent)
    }
}

class ImeServiceDisabled : RecoverableFailure(appStr(R.string.error_ime_service_disabled)) {
    override suspend fun recover(fragment: Fragment) {
        KeyboardUtils.openImeSettings()
    }
}

class ImeServiceNotChosen : RecoverableFailure(appStr(R.string.error_ime_must_be_chosen)) {
    @ExperimentalSplittiesApi
    override suspend fun recover(fragment: Fragment) {
        if (isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)) {
            KeyboardUtils.switchToKeyMapperIme()
        } else {
            KeyboardUtils.showInputMethodPicker()
        }
    }
}

class OptionsNotRequired : Failure(appStr(R.string.error_options_not_required))
class SystemFeatureNotSupported(feature: String) : Failure(appStr(R.string.error_feature_not_available, feature))
class ConstraintNotFound : Failure(appStr(R.string.error_constraint_not_found))
class ExtraNotFound(extraId: String) : Failure(appStr(R.string.error_extra_not_found, extraId))
class NoActionData : Failure(appStr(R.string.error_no_action_data))
class FlagNotFound : Failure(appStr(R.string.error_flag_not_found))
class InvalidActionType(actionType: ActionType) : Failure(appStr(R.string.error_invalid_action_type, actionType.toString()))

class SdkVersionTooLow(sdkVersion: Int
) : Failure(appStr(R.string.error_sdk_version_too_low, BuildUtils.getSdkVersionName(sdkVersion)))

class SdkVersionTooHigh(sdkVersion: Int
) : Failure(appStr(R.string.error_sdk_version_too_high, BuildUtils.getSdkVersionName(sdkVersion)))

class FeatureUnavailable(feature: String) : Failure(appStr(R.string.error_feature_not_available, feature))
class SystemActionNotFound(id: String) : Failure(appStr(R.string.error_system_action_not_found, id))
class KeyMapperImeNotFound : Failure(appStr(R.string.error_key_mapper_ime_not_found))
class InputMethodNotFound(id: String) : Failure(appStr(R.string.error_ime_not_found, id))
class OptionLabelNotFound(id: String) : Failure(appStr(R.string.error_cant_find_option_label, id))
class NoEnabledInputMethods : Failure(appStr(R.string.error_no_enabled_imes))
class GoogleAppNotFound : RecoverableFailure(appStr(R.string.error_google_app_not_installed)) {
    override suspend fun recover(fragment: Fragment) {

        AppNotFound(appStr(R.string.google_app_package_name)).recover(fragment)
    }
}

class FrontFlashNotFound : Failure(appStr(R.string.error_front_flash_not_found))
class BackFlashNotFound : Failure(appStr(R.string.error_back_flash_not_found))
class ImeNotFound(id: String) : Failure(appStr(R.string.error_ime_not_found, id))