package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.util.ui.ResourceProvider

/**
 * Created by sds100 on 26/02/2020.
 */

/**
 * Inspired from @antonyharfield great example!
 */

sealed class Result<out T>

data class Success<T>(val value: T) : Result<T>()

sealed class Error : Result<Nothing>() {
    data class Exception(val exception: java.lang.Exception) : Error()
    data class SystemFeatureNotSupported(val feature: String) : Error()
    data class ExtraNotFound(val extraId: String) : Error()
    data class SdkVersionTooLow(val minSdk: Int) : Error()
    data class SdkVersionTooHigh(val maxSdk: Int) : Error()
    data class InputMethodNotFound(val imeLabel: String) : Error()
    object NoVoiceAssistant : Error()
    object NoDeviceAssistant : Error()
    object NoCameraApp : Error()
    object NoSettingsApp : Error()
    object FrontFlashNotFound : Error()
    object BackFlashNotFound : Error()
    data class ImeDisabled(val ime: ImeInfo) : Error()
    data class DeviceNotFound(val descriptor: String) : Error()
    object InvalidNumber : Error()
    data class NumberTooBig(val max: Int) : Error()
    data class NumberTooSmall(val min: Int) : Error()
    object EmptyText : Error()
    object NoIncompatibleKeyboardsInstalled : Error()
    object NoMediaSessions : Error()
    object BackupVersionTooNew : Error()
    object LauncherShortcutsNotSupported : Error()

    data class AppNotFound(val packageName: String) : Error()
    data class AppDisabled(val packageName: String) : Error()
    object AppShortcutCantBeOpened : Error()
    object InsufficientPermissionsToOpenAppShortcut : Error()
    object NoCompatibleImeEnabled : Error()
    object NoCompatibleImeChosen : Error()

    object AccessibilityServiceDisabled : Error()
    object AccessibilityServiceCrashed : Error()

    object CantShowImePickerInBackground : Error()
    object CantFindImeSettings : Error()
    object GestureStrokeCountTooHigh : Error()
    object GestureDurationTooHigh : Error()

    data class PermissionDenied(val permission: Permission) : Error() {
        companion object {

            fun getMessageForPermission(
                resourceProvider: ResourceProvider,
                permission: Permission
            ): String {
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
                }

                return resourceProvider.getString(resId)
            }
        }
    }

    object FailedToFindAccessibilityNode : Error()
    data class FailedToPerformAccessibilityGlobalAction(val action: Int) : Error()
    object FailedToDispatchGesture : Error()

    object CameraInUse : Error()
    object CameraDisconnected : Error()
    object CameraDisabled : Error()
    object MaxCamerasInUse : Error()
    object CameraError : Error()

    data class FailedToModifySystemSetting(val setting: String) : Error()
    object FailedToChangeIme : Error()
    object NoAppToOpenUrl : Error()
    object NoAppToPhoneCall : Error()

    data class NotAFile(val uri: String) : Error()
    data class NotADirectory(val uri: String) : Error()
    object StoragePermissionDenied : Error()
    data class CannotCreateFileInTarget(val uri: String) : Error()
    data class SourceFileNotFound(val uri: String) : Error()
    data class TargetFileNotFound(val uri: String) : Error()
    data class TargetDirectoryNotFound(val uri: String) : Error()
    object UnknownIOError : Error()
    object FileOperationCancelled : Error()
    object TargetDirectoryMatchesSourceDirectory : Error()
    data class NoSpaceLeftOnTarget(val uri: String) : Error()
    object NoFileName : Error()

    object EmptyJson : Error()
    object CantFindSoundFile : Error()
    data class CorruptJsonFile(val reason: String) : Error()

    object ShizukuNotStarted : Error()
    object CantDetectKeyEventsInPhoneCall : Error()
}

inline fun <T> Result<T>.onSuccess(f: (T) -> Unit): Result<T> {
    if (this is Success) {
        f(this.value)
    }

    return this
}

inline fun <T, U> Result<T>.onFailure(f: (error: Error) -> U): Result<T> {
    if (this is Error) {
        f(this)
    }

    return this
}

inline infix fun <T, U> Result<T>.then(f: (T) -> Result<U>) =
    when (this) {
        is Success -> f(this.value)
        is Error -> this
    }

suspend infix fun <T, U> Result<T>.suspendThen(f: suspend (T) -> Result<U>) =
    when (this) {
        is Success -> f(this.value)
        is Error -> this
    }

inline infix fun <T> Result<T>.otherwise(f: (error: Error) -> Result<T>) =
    when (this) {
        is Success -> this
        is Error -> f(this)
    }

inline infix fun <T> Result<T>.valueIfFailure(f: (error: Error) -> T): T =
    when (this) {
        is Success -> this.value
        is Error -> f(this)
    }

fun <T> Result<T>.errorOrNull(): Error? {
    when (this) {
        is Error -> return this
        else -> Unit
    }

    return null
}

fun <T> Result<T>.valueOrNull(): T? {
    when (this) {
        is Success -> return this.value
        else -> Unit
    }

    return null
}

val <T> Result<T>.isError: Boolean
    get() = this is Error

val <T> Result<T>.isSuccess: Boolean
    get() = this is Success

fun <T, U> Result<T>.handle(onSuccess: (value: T) -> U, onError: (error: Error) -> U): U {
    return when (this) {
        is Success -> onSuccess(value)
        is Error -> onError(this)
    }
}

fun <T> T.success() = Success(this)