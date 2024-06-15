package io.github.sds100.keymapper.util

import android.content.pm.PackageManager
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.BuildUtils
import io.github.sds100.keymapper.util.ui.ResourceProvider

/**
 * Created by sds100 on 29/02/2020.
 */

fun Error.getFullMessage(resourceProvider: ResourceProvider) = when (this) {
    is Error.PermissionDenied ->
        Error.PermissionDenied.getMessageForPermission(
            resourceProvider,
            permission,
        )

    is Error.AppNotFound -> resourceProvider.getString(
        R.string.error_app_isnt_installed,
        packageName,
    )

    is Error.AppDisabled -> resourceProvider.getString(
        R.string.error_app_is_disabled_package_name,
        this.packageName,
    )

    is Error.NoCompatibleImeEnabled -> resourceProvider.getString(R.string.error_key_mapper_ime_service_disabled)
    is Error.NoCompatibleImeChosen -> resourceProvider.getString(R.string.error_ime_must_be_chosen)
    is Error.SystemFeatureNotSupported -> when (this.feature) {
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

    is Error.ExtraNotFound -> resourceProvider.getString(R.string.error_extra_not_found, extraId)
    is Error.SdkVersionTooLow -> resourceProvider.getString(
        R.string.error_sdk_version_too_low,
        BuildUtils.getSdkVersionName(minSdk),
    )

    is Error.SdkVersionTooHigh -> resourceProvider.getString(
        R.string.error_sdk_version_too_high,
        BuildUtils.getSdkVersionName(maxSdk),
    )

    is Error.InputMethodNotFound -> resourceProvider.getString(
        R.string.error_ime_not_found,
        imeLabel,
    )

    is Error.FrontFlashNotFound -> resourceProvider.getString(R.string.error_front_flash_not_found)
    is Error.BackFlashNotFound -> resourceProvider.getString(R.string.error_back_flash_not_found)
    is Error.DeviceNotFound -> resourceProvider.getString(R.string.error_device_not_found)
    is Error.Exception -> exception.toString()
    is Error.EmptyJson -> resourceProvider.getString(R.string.error_empty_json)
    is Error.InvalidNumber -> resourceProvider.getString(R.string.error_invalid_number)
    is Error.NumberTooSmall -> resourceProvider.getString(R.string.error_number_too_small, min)
    is Error.NumberTooBig -> resourceProvider.getString(R.string.error_number_too_big, max)
    is Error.EmptyText -> resourceProvider.getString(R.string.error_cant_be_empty)
    Error.BackupVersionTooNew -> resourceProvider.getString(R.string.error_backup_version_too_new)
    Error.NoIncompatibleKeyboardsInstalled -> resourceProvider.getString(R.string.error_no_incompatible_input_methods_installed)
    Error.NoMediaSessions -> resourceProvider.getString(R.string.error_no_media_sessions)
    Error.NoVoiceAssistant -> resourceProvider.getString(R.string.error_voice_assistant_not_found)
    Error.AccessibilityServiceDisabled -> resourceProvider.getString(R.string.error_accessibility_service_disabled)
    Error.LauncherShortcutsNotSupported -> resourceProvider.getString(R.string.error_launcher_shortcuts_not_supported)
    Error.AccessibilityServiceCrashed -> resourceProvider.getString(R.string.error_accessibility_service_crashed)
    Error.CantFindImeSettings -> resourceProvider.getString(R.string.error_cant_find_ime_settings)
    Error.CantShowImePickerInBackground -> resourceProvider.getString(R.string.error_cant_show_ime_picker_in_background)
    Error.FailedToFindAccessibilityNode -> resourceProvider.getString(R.string.error_failed_to_find_accessibility_node)
    Error.AccessibilityNodeNotVisible -> resourceProvider.getString(R.string.error_accessibility_node_not_visible)
    is Error.FailedToPerformAccessibilityGlobalAction -> resourceProvider.getString(
        R.string.error_failed_to_perform_accessibility_global_action,
        action,
    )

    Error.FailedToDispatchGesture -> resourceProvider.getString(R.string.error_failed_to_dispatch_gesture)
    Error.AppShortcutCantBeOpened -> resourceProvider.getString(R.string.error_opening_app_shortcut)
    Error.InsufficientPermissionsToOpenAppShortcut -> resourceProvider.getString(R.string.error_keymapper_doesnt_have_permission_app_shortcut)
    Error.NoAppToPhoneCall -> resourceProvider.getString(R.string.error_no_app_to_phone_call)

    Error.CameraInUse -> resourceProvider.getString(R.string.error_camera_in_use)
    Error.CameraError -> resourceProvider.getString(R.string.error_camera_error)
    Error.CameraDisabled -> resourceProvider.getString(R.string.error_camera_disabled)
    Error.CameraDisconnected -> resourceProvider.getString(R.string.error_camera_disconnected)
    Error.MaxCamerasInUse -> resourceProvider.getString(R.string.error_max_cameras_in_use)
    is Error.FailedToModifySystemSetting -> resourceProvider.getString(
        R.string.error_failed_to_modify_system_setting,
        setting,
    )

    is Error.ImeDisabled -> resourceProvider.getString(R.string.error_ime_disabled, this.ime.label)
    Error.FailedToChangeIme -> resourceProvider.getString(R.string.error_failed_to_change_ime)
    Error.NoCameraApp -> resourceProvider.getString(R.string.error_no_camera_app)
    Error.NoDeviceAssistant -> resourceProvider.getString(R.string.error_no_device_assistant)
    Error.NoSettingsApp -> resourceProvider.getString(R.string.error_no_settings_app)
    Error.NoAppToOpenUrl -> resourceProvider.getString(R.string.error_no_app_to_open_url)

    Error.CantFindSoundFile -> resourceProvider.getString(R.string.error_cant_find_sound_file)
    is Error.CorruptJsonFile -> reason

    is Error.CannotCreateFileInTarget -> resourceProvider.getString(
        R.string.error_file_access_denied,
        uri,
    )

    Error.FileOperationCancelled -> resourceProvider.getString(R.string.error_file_operation_cancelled)
    is Error.NoSpaceLeftOnTarget -> resourceProvider.getString(
        R.string.error_no_space_left_at_target,
        uri,
    )

    is Error.NotADirectory -> resourceProvider.getString(R.string.error_not_a_directory, uri)
    is Error.NotAFile -> resourceProvider.getString(R.string.error_not_a_file, uri)
    is Error.SourceFileNotFound -> resourceProvider.getString(
        R.string.error_source_file_not_found,
        uri,
    )

    Error.StoragePermissionDenied -> resourceProvider.getString(R.string.error_storage_permission_denied)
    Error.TargetDirectoryMatchesSourceDirectory -> resourceProvider.getString(R.string.error_matching_source_and_target_paths)
    is Error.TargetDirectoryNotFound -> resourceProvider.getString(
        R.string.error_directory_not_found,
        uri,
    )

    is Error.TargetFileNotFound -> resourceProvider.getString(
        R.string.error_target_file_not_found,
        uri,
    )

    Error.UnknownIOError -> resourceProvider.getString(R.string.error_io_error)
    Error.ShizukuNotStarted -> resourceProvider.getString(R.string.error_shizuku_not_started)
    Error.NoFileName -> resourceProvider.getString(R.string.error_no_file_name)
    Error.CantDetectKeyEventsInPhoneCall -> resourceProvider.getString(R.string.trigger_error_cant_detect_in_phone_call_explanation)
    Error.GestureStrokeCountTooHigh -> resourceProvider.getString(R.string.trigger_error_gesture_stroke_count_too_high)
    Error.GestureDurationTooHigh -> resourceProvider.getString(R.string.trigger_error_gesture_duration_too_high)
}

val Error.isFixable: Boolean
    get() = when (this) {
        is Error.AppNotFound,
        is Error.AppDisabled,
        Error.NoCompatibleImeEnabled,
        Error.NoCompatibleImeChosen,
        is Error.ImeDisabled,
        Error.AccessibilityServiceDisabled,
        Error.AccessibilityServiceCrashed,
        is Error.PermissionDenied,
        is Error.ShizukuNotStarted,
        is Error.CantDetectKeyEventsInPhoneCall,
        -> true

        else -> false
    }
