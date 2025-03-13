package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.purchasing.ProductId
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
    data object NoVoiceAssistant : Error()
    data object NoDeviceAssistant : Error()
    data object NoCameraApp : Error()
    data object NoSettingsApp : Error()
    data object FrontFlashNotFound : Error()
    data object BackFlashNotFound : Error()
    data class ImeDisabled(val ime: ImeInfo) : Error()
    data class DeviceNotFound(val descriptor: String) : Error()
    data object InvalidNumber : Error()
    data class NumberTooBig(val max: Int) : Error()
    data class NumberTooSmall(val min: Int) : Error()
    data object EmptyText : Error()
    data object NoIncompatibleKeyboardsInstalled : Error()
    data object NoMediaSessions : Error()
    data object BackupVersionTooNew : Error()
    data object LauncherShortcutsNotSupported : Error()

    data class AppNotFound(val packageName: String) : Error()
    data class AppDisabled(val packageName: String) : Error()
    data object AppShortcutCantBeOpened : Error()
    data object InsufficientPermissionsToOpenAppShortcut : Error()
    data object NoCompatibleImeEnabled : Error()
    data object NoCompatibleImeChosen : Error()

    data object AccessibilityServiceDisabled : Error()
    data object AccessibilityServiceCrashed : Error()

    data object CantShowImePickerInBackground : Error()
    data object CantFindImeSettings : Error()
    data object GestureStrokeCountTooHigh : Error()
    data object GestureDurationTooHigh : Error()

    data class PermissionDenied(val permission: Permission) : Error() {
        companion object {

            fun getMessageForPermission(
                resourceProvider: ResourceProvider,
                permission: Permission,
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
                    Permission.POST_NOTIFICATIONS -> R.string.error_notifications_permission_denied
                }

                return resourceProvider.getString(resId)
            }
        }
    }

    data object FailedToFindAccessibilityNode : Error()
    data class FailedToPerformAccessibilityGlobalAction(val action: Int) : Error()
    data object FailedToDispatchGesture : Error()

    data object CameraInUse : Error()
    data object CameraDisconnected : Error()
    data object CameraDisabled : Error()
    data object MaxCamerasInUse : Error()
    data object CameraError : Error()

    data class FailedToModifySystemSetting(val setting: String) : Error()
    data object FailedToChangeIme : Error()
    data object NoAppToOpenUrl : Error()
    data object NoAppToPhoneCall : Error()

    data class NotAFile(val uri: String) : Error()
    data class NotADirectory(val uri: String) : Error()
    data object StoragePermissionDenied : Error()
    data class CannotCreateFileInTarget(val uri: String) : Error()
    data class SourceFileNotFound(val uri: String) : Error()
    data class TargetFileNotFound(val uri: String) : Error()
    data class TargetDirectoryNotFound(val uri: String) : Error()
    data object UnknownIOError : Error()
    data object FileOperationCancelled : Error()
    data object TargetDirectoryMatchesSourceDirectory : Error()
    data class NoSpaceLeftOnTarget(val uri: String) : Error()
    data object NoFileName : Error()

    data object EmptyJson : Error()
    data object CantFindSoundFile : Error()
    data class CorruptJsonFile(val reason: String) : Error()

    data object ShizukuNotStarted : Error()
    data object CantDetectKeyEventsInPhoneCall : Error()

    // This is returned from the PurchasingManager on FOSS builds that don't
    // have the pro features implemented.
    data object PurchasingNotImplemented : Error()

    data class ProductNotPurchased(val product: ProductId) : Error()

    sealed class PurchasingError : Error() {
        data object ProductNotFound : PurchasingError()
        data object Cancelled : PurchasingError()
        data object StoreProblem : PurchasingError()
        data object NetworkError : PurchasingError()
        data object PaymentPending : PurchasingError()
        data class Unexpected(val message: String) : PurchasingError()
    }

    /**
     * DPAD triggers require a Key Mapper keyboard to be selected.
     */
    data object DpadTriggerImeNotSelected : Error()
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

inline fun <T, U> Result<T>.resolve(
    onSuccess: (value: T) -> U,
    onFailure: (error: Error) -> U,
) =
    when (this) {
        is Success -> onSuccess(this.value)
        is Error -> onFailure(this)
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

fun <T, U> Result<T>.handle(onSuccess: (value: T) -> U, onError: (error: Error) -> U): U =
    when (this) {
        is Success -> onSuccess(value)
        is Error -> onError(this)
    }

fun <T> T.success() = Success(this)
