package io.github.sds100.keymapper.base.utils

import android.content.pm.PackageManager
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.BuildUtils
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.data.DataError
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission

fun KMError.getFullMessage(resourceProvider: ResourceProvider): String {
    return when (this) {
        is SystemError.PermissionDenied -> {
            val resId = when (permission) {
                Permission.WRITE_SETTINGS -> R.string.error_action_requires_write_settings_permission
                Permission.CAMERA -> R.string.error_action_requires_camera_permission
                Permission.DEVICE_ADMIN -> R.string.error_need_to_enable_device_admin
                Permission.READ_PHONE_STATE -> R.string.error_action_requires_read_phone_state_permission
                Permission.ACCESS_NOTIFICATION_POLICY -> R.string.error_action_notification_policy_permission
                Permission.WRITE_SECURE_SETTINGS -> R.string.error_need_write_secure_settings_permission
                Permission.NOTIFICATION_LISTENER -> R.string.error_denied_notification_listener_service_permission
                Permission.CALL_PHONE -> R.string.error_denied_call_phone_permission
                Permission.ROOT -> R.string.error_requires_root
                Permission.IGNORE_BATTERY_OPTIMISATION -> R.string.error_battery_optimisation_enabled
                Permission.SHIZUKU -> R.string.error_shizuku_permission_denied
                Permission.ACCESS_FINE_LOCATION -> R.string.error_access_fine_location_permission_denied
                Permission.ANSWER_PHONE_CALL -> R.string.error_answer_end_phone_call_permission_denied
                Permission.FIND_NEARBY_DEVICES -> R.string.error_find_nearby_devices_permission_denied
                Permission.POST_NOTIFICATIONS -> R.string.error_notifications_permission_denied
            }

            resourceProvider.getString(resId)
        }

        is KMError.AppNotFound -> resourceProvider.getString(
            R.string.error_app_isnt_installed,
            packageName,
        )

        is KMError.AppDisabled -> resourceProvider.getString(
            R.string.error_app_is_disabled_package_name,
            this.packageName,
        )

        is KMError.NoCompatibleImeEnabled -> resourceProvider.getString(R.string.error_key_mapper_ime_service_disabled)
        is KMError.NoCompatibleImeChosen -> resourceProvider.getString(R.string.error_ime_must_be_chosen)
        is KMError.SystemFeatureNotSupported -> when (this.feature) {
            PackageManager.FEATURE_NFC -> resourceProvider.getString(R.string.error_system_feature_nfc_unsupported)
            PackageManager.FEATURE_CAMERA -> resourceProvider.getString(R.string.error_system_feature_camera_unsupported)
            PackageManager.FEATURE_FINGERPRINT -> resourceProvider.getString(R.string.error_system_feature_fingerprint_unsupported)
            PackageManager.FEATURE_WIFI -> resourceProvider.getString(R.string.error_system_feature_wifi_unsupported)
            PackageManager.FEATURE_BLUETOOTH -> resourceProvider.getString(R.string.error_system_feature_bluetooth_unsupported)
            PackageManager.FEATURE_DEVICE_ADMIN -> resourceProvider.getString(R.string.error_system_feature_device_admin_unsupported)
            PackageManager.FEATURE_CAMERA_FLASH -> resourceProvider.getString(R.string.error_system_feature_camera_flash_unsupported)
            PackageManager.FEATURE_TELEPHONY -> resourceProvider.getString(R.string.error_system_feature_telephony_unsupported)
            else -> throw Exception("Don't know how to get error message for this system feature ${this.feature}")
        }

        is DataError.ExtraNotFound -> resourceProvider.getString(
            R.string.error_extra_not_found,
            extraId,
        )

        is KMError.SdkVersionTooLow -> resourceProvider.getString(
            R.string.error_sdk_version_too_low,
            BuildUtils.getSdkVersionName(minSdk),
        )

        is KMError.SdkVersionTooHigh -> resourceProvider.getString(
            R.string.error_sdk_version_too_high,
            BuildUtils.getSdkVersionName(maxSdk),
        )

        is KMError.InputMethodNotFound -> resourceProvider.getString(
            R.string.error_ime_not_found,
            imeLabel,
        )

        is KMError.FrontFlashNotFound -> resourceProvider.getString(R.string.error_front_flash_not_found)
        is KMError.BackFlashNotFound -> resourceProvider.getString(R.string.error_back_flash_not_found)
        is KMError.DeviceNotFound -> resourceProvider.getString(R.string.error_device_not_found)
        is KMError.Exception -> exception.toString()
        is KMError.EmptyJson -> resourceProvider.getString(R.string.error_empty_json)
        is KMError.InvalidNumber -> resourceProvider.getString(R.string.error_invalid_number)
        is KMError.NumberTooSmall -> resourceProvider.getString(R.string.error_number_too_small, min)
        is KMError.NumberTooBig -> resourceProvider.getString(R.string.error_number_too_big, max)
        is KMError.EmptyText -> resourceProvider.getString(R.string.error_cant_be_empty)
        KMError.BackupVersionTooNew -> resourceProvider.getString(R.string.error_backup_version_too_new)
        KMError.NoIncompatibleKeyboardsInstalled -> resourceProvider.getString(R.string.error_no_incompatible_input_methods_installed)
        KMError.NoMediaSessions -> resourceProvider.getString(R.string.error_no_media_sessions)
        KMError.NoVoiceAssistant -> resourceProvider.getString(R.string.error_voice_assistant_not_found)
        KMError.AccessibilityServiceDisabled -> resourceProvider.getString(R.string.error_accessibility_service_disabled)
        KMError.LauncherShortcutsNotSupported -> resourceProvider.getString(R.string.error_launcher_shortcuts_not_supported)
        KMError.AccessibilityServiceCrashed -> resourceProvider.getString(R.string.error_accessibility_service_crashed)
        KMError.CantFindImeSettings -> resourceProvider.getString(R.string.error_cant_find_ime_settings)
        KMError.CantShowImePickerInBackground -> resourceProvider.getString(R.string.error_cant_show_ime_picker_in_background)
        KMError.FailedToFindAccessibilityNode -> resourceProvider.getString(R.string.error_failed_to_find_accessibility_node)
        is KMError.FailedToPerformAccessibilityGlobalAction -> resourceProvider.getString(
            R.string.error_failed_to_perform_accessibility_global_action,
            action,
        )

        KMError.FailedToDispatchGesture -> resourceProvider.getString(R.string.error_failed_to_dispatch_gesture)
        KMError.AppShortcutCantBeOpened -> resourceProvider.getString(R.string.error_opening_app_shortcut)
        KMError.InsufficientPermissionsToOpenAppShortcut -> resourceProvider.getString(R.string.error_keymapper_doesnt_have_permission_app_shortcut)
        KMError.NoAppToPhoneCall -> resourceProvider.getString(R.string.error_no_app_to_phone_call)

        KMError.CameraInUse -> resourceProvider.getString(R.string.error_camera_in_use)
        KMError.CameraError -> resourceProvider.getString(R.string.error_camera_error)
        KMError.CameraDisabled -> resourceProvider.getString(R.string.error_camera_disabled)
        KMError.CameraDisconnected -> resourceProvider.getString(R.string.error_camera_disconnected)
        KMError.MaxCamerasInUse -> resourceProvider.getString(R.string.error_max_cameras_in_use)
        KMError.CameraVariableFlashlightStrengthUnsupported -> resourceProvider.getString(R.string.error_variable_flashlight_strength_unsupported)

        is KMError.FailedToModifySystemSetting -> resourceProvider.getString(
            R.string.error_failed_to_modify_system_setting,
            setting,
        )

        is SystemError.ImeDisabled -> resourceProvider.getString(
            R.string.error_ime_disabled,
            this.ime.label,
        )

        KMError.FailedToChangeIme -> resourceProvider.getString(R.string.error_failed_to_change_ime)
        KMError.NoCameraApp -> resourceProvider.getString(R.string.error_no_camera_app)
        KMError.NoDeviceAssistant -> resourceProvider.getString(R.string.error_no_device_assistant)
        KMError.NoSettingsApp -> resourceProvider.getString(R.string.error_no_settings_app)
        KMError.NoAppToOpenUrl -> resourceProvider.getString(R.string.error_no_app_to_open_url)

        KMError.CantFindSoundFile -> resourceProvider.getString(R.string.error_cant_find_sound_file)
        is KMError.CorruptJsonFile -> reason

        is KMError.CannotCreateFileInTarget -> resourceProvider.getString(
            R.string.error_file_access_denied,
            uri,
        )

        KMError.FileOperationCancelled -> resourceProvider.getString(R.string.error_file_operation_cancelled)
        is KMError.NoSpaceLeftOnTarget -> resourceProvider.getString(
            R.string.error_no_space_left_at_target,
            uri,
        )

        is KMError.NotADirectory -> resourceProvider.getString(R.string.error_not_a_directory, uri)
        is KMError.NotAFile -> resourceProvider.getString(R.string.error_not_a_file, uri)
        is KMError.SourceFileNotFound -> resourceProvider.getString(
            R.string.error_source_file_not_found,
            uri,
        )

        KMError.StoragePermissionDenied -> resourceProvider.getString(R.string.error_storage_permission_denied)
        KMError.TargetDirectoryMatchesSourceDirectory -> resourceProvider.getString(R.string.error_matching_source_and_target_paths)
        is KMError.TargetDirectoryNotFound -> resourceProvider.getString(
            R.string.error_directory_not_found,
            uri,
        )

        is KMError.TargetFileNotFound -> resourceProvider.getString(
            R.string.error_target_file_not_found,
            uri,
        )

        KMError.UnknownIOError -> resourceProvider.getString(R.string.error_io_error)
        KMError.ShizukuNotStarted -> resourceProvider.getString(R.string.error_shizuku_not_started)
        KMError.NoFileName -> resourceProvider.getString(R.string.error_no_file_name)
        KMError.CantDetectKeyEventsInPhoneCall -> resourceProvider.getString(R.string.trigger_error_cant_detect_in_phone_call_explanation)
        KMError.GestureStrokeCountTooHigh -> resourceProvider.getString(R.string.trigger_error_gesture_stroke_count_too_high)
        KMError.GestureDurationTooHigh -> resourceProvider.getString(R.string.trigger_error_gesture_duration_too_high)

        KMError.DpadTriggerImeNotSelected -> resourceProvider.getString(R.string.trigger_error_dpad_ime_not_selected)
        KMError.InvalidBackup -> resourceProvider.getString(R.string.error_invalid_backup)
        KMError.MalformedUrl -> resourceProvider.getString(R.string.error_malformed_url)
        KMError.UiElementNotFound -> resourceProvider.getString(R.string.error_ui_element_not_found)

        else -> this.toString()
    }
}

val KMError.isFixable: Boolean
    get() = when (this) {
        is KMError.AppNotFound,
        is KMError.AppDisabled,
        KMError.NoCompatibleImeEnabled,
        KMError.NoCompatibleImeChosen,
        is SystemError.ImeDisabled,
        KMError.AccessibilityServiceDisabled,
        KMError.AccessibilityServiceCrashed,
        is SystemError.PermissionDenied,
        is KMError.ShizukuNotStarted,
        is KMError.CantDetectKeyEventsInPhoneCall,

        -> true

        else -> false
    }
