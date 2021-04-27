package io.github.sds100.keymapper.util

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
            permission
        )
    is Error.AppNotFound -> resourceProvider.getString(
        R.string.error_app_isnt_installed,
        packageName
    )
    is Error.AppDisabled -> resourceProvider.getString(R.string.error_app_is_disabled)
    is Error.NoCompatibleImeEnabled -> resourceProvider.getString(R.string.error_key_mapper_ime_service_disabled)
    is Error.NoCompatibleImeChosen -> resourceProvider.getString(R.string.error_ime_must_be_chosen)
    is Error.SystemFeatureNotSupported -> resourceProvider.getString(
        R.string.error_feature_not_available,
        feature
    )
    is Error.ExtraNotFound -> resourceProvider.getString(R.string.error_extra_not_found, extraId)
    is Error.SdkVersionTooLow -> resourceProvider.getString(
        R.string.error_sdk_version_too_low,
        BuildUtils.getSdkVersionName(minSdk)
    )
    is Error.SdkVersionTooHigh -> resourceProvider.getString(
        R.string.error_sdk_version_too_high,
        BuildUtils.getSdkVersionName(maxSdk)
    )
    is Error.FeatureUnavailable -> resourceProvider.getString(
        R.string.error_feature_not_available,
        feature
    )
    is Error.KeyMapperImeNotFound -> resourceProvider.getString(R.string.error_key_mapper_ime_not_found)
    is Error.InputMethodNotFound -> resourceProvider.getString(R.string.error_ime_not_found, id)
    is Error.NoEnabledInputMethods -> resourceProvider.getString(R.string.error_no_enabled_imes)
    is Error.FrontFlashNotFound -> resourceProvider.getString(R.string.error_front_flash_not_found)
    is Error.BackFlashNotFound -> resourceProvider.getString(R.string.error_back_flash_not_found)
    is Error.DownloadFailed -> resourceProvider.getString(R.string.error_download_failed)
    is Error.FileNotCached -> resourceProvider.getString(R.string.error_file_not_cached)
    is Error.SSLHandshakeError -> resourceProvider.getString(R.string.error_ssl_handshake_exception)
    is Error.DeviceNotFound -> resourceProvider.getString(R.string.error_device_not_found)
    is Error.Exception -> exception.toString()
    is Error.EmptyJson -> resourceProvider.getString(R.string.error_empty_json)
    is Error.FileAccessDenied -> resourceProvider.getString(R.string.error_file_access_denied)
    is Error.FailedToSplitString -> resourceProvider.getString(
        R.string.error_failed_to_split_string,
        string
    )
    is Error.InvalidNumber -> resourceProvider.getString(R.string.error_invalid_number)
    is Error.NumberTooSmall -> resourceProvider.getString(R.string.error_number_too_small, min)
    is Error.NumberTooBig -> resourceProvider.getString(R.string.error_number_too_big, max)
    is Error.CantBeEmpty -> resourceProvider.getString(R.string.error_cant_be_empty)
    Error.BackupVersionTooNew -> resourceProvider.getString(R.string.error_backup_version_too_new)
    Error.CorruptActionError -> resourceProvider.getString(R.string.error_corrupt_action)
    is Error.CorruptJsonFile -> reason
    Error.NoIncompatibleKeyboardsInstalled -> resourceProvider.getString(R.string.error_no_incompatible_input_methods_installed)
    Error.NoMediaSessions -> resourceProvider.getString(R.string.error_no_media_sessions)
    Error.NoVoiceAssistant -> resourceProvider.getString(R.string.error_voice_assistant_not_found)
    is Error.UnknownFileLocation -> resourceProvider.getString(R.string.error_unknown_file_location)
    Error.AccessibilityServiceDisabled -> resourceProvider.getString(R.string.error_accessibility_service_disabled)
    Error.Duplicate -> resourceProvider.getString(R.string.error_duplicate_constraint)
    Error.LauncherShortcutsNotSupported -> resourceProvider.getString(R.string.error_launcher_shortcuts_not_supported)
    Error.AccessibilityServiceCrashed -> resourceProvider.getString(R.string.error_accessibility_service_crashed)
    Error.CantFindImeSettings -> resourceProvider.getString(R.string.error_cant_find_ime_settings)
    Error.CantShowImePickerInBackground -> resourceProvider.getString(R.string.error_cant_show_ime_picker_in_background)
    Error.FailedToFindAccessibilityNode -> resourceProvider.getString(R.string.error_failed_to_find_accessibility_node)
    is Error.FailedToPerformAccessibilityGlobalAction -> resourceProvider.getString(
        R.string.error_failed_to_perform_accessibility_global_action,
        action
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
        setting
    )
    is Error.ImeDisabled -> resourceProvider.getString(R.string.error_ime_disabled, this.ime.label)
    Error.FailedToChangeIme -> resourceProvider.getString(R.string.error_failed_to_change_ime)
    Error.NoCameraApp -> resourceProvider.getString(R.string.error_no_camera_app)
    Error.NoDeviceAssistant -> resourceProvider.getString(R.string.error_no_device_assistant)
    Error.NoSettingsApp -> resourceProvider.getString(R.string.error_no_settings_app)
    Error.NoAppToOpenUrl -> resourceProvider.getString(R.string.error_no_app_to_open_url)
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
        is Error.PermissionDenied
        -> true

        else -> false
    }