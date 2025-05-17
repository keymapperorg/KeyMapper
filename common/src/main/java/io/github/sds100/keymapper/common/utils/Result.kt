package io.github.sds100.keymapper.common.utils

/**
 * Inspired from @antonyharfield great example!
 */

// TODO rename KMResult
sealed class Result<out T>

data class Success<T>(val value: T) : Result<T>()

// TODO move all these errors to their respective packages

abstract class Error : Result<Nothing>() {
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

    data object FailedToFindAccessibilityNode : Error()
    data class FailedToPerformAccessibilityGlobalAction(val action: Int) : Error()
    data object FailedToDispatchGesture : Error()

    data object CameraInUse : Error()
    data object CameraDisconnected : Error()
    data object CameraDisabled : Error()
    data object MaxCamerasInUse : Error()
    data object CameraError : Error()
    data object CameraVariableFlashlightStrengthUnsupported : Error()

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
    data object InvalidBackup : Error()

    data object EmptyJson : Error()
    data object CantFindSoundFile : Error()
    data class CorruptJsonFile(val reason: String) : Error()

    data object ShizukuNotStarted : Error()
    data object CantDetectKeyEventsInPhoneCall : Error()

    /**
     * DPAD triggers require a Key Mapper keyboard to be selected.
     */
    data object DpadTriggerImeNotSelected : Error()
    data object MalformedUrl : Error()

    data object UiElementNotFound : Error()
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

inline infix fun <T, U> Result<T>.then(f: (T) -> Result<U>) = when (this) {
    is Success -> f(this.value)
    is Error -> this
}

suspend infix fun <T, U> Result<T>.suspendThen(f: suspend (T) -> Result<U>) = when (this) {
    is Success -> f(this.value)
    is Error -> this
}

inline infix fun <T> Result<T>.otherwise(f: (error: Error) -> Result<T>) = when (this) {
    is Success -> this
    is Error -> f(this)
}

inline fun <T, U> Result<T>.resolve(
    onSuccess: (value: T) -> U,
    onFailure: (error: Error) -> U,
) = when (this) {
    is Success -> onSuccess(this.value)
    is Error -> onFailure(this)
}

inline infix fun <T> Result<T>.valueIfFailure(f: (error: Error) -> T): T = when (this) {
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

fun <T, U> Result<T>.handle(onSuccess: (value: T) -> U, onError: (error: Error) -> U): U = when (this) {
    is Success -> onSuccess(value)
    is Error -> onError(this)
}

fun <T> T.success() = Success(this)
