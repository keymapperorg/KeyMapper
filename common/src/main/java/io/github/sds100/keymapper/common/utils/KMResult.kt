package io.github.sds100.keymapper.common.utils

/**
 * Inspired from @antonyharfield great example!
 */

sealed class KMResult<out T>

data class Success<T>(val value: T) : KMResult<T>()

abstract class KMError : KMResult<Nothing>() {
    data class Exception(val exception: java.lang.Exception) : KMError()
    data class SystemFeatureNotSupported(val feature: String) : KMError()
    data class SdkVersionTooLow(val minSdk: Int) : KMError()
    data class SdkVersionTooHigh(val maxSdk: Int) : KMError()
    data class InputMethodNotFound(val imeLabel: String) : KMError()
    data object NoVoiceAssistant : KMError()
    data object NoDeviceAssistant : KMError()
    data object NoCameraApp : KMError()
    data object NoSettingsApp : KMError()
    data object FrontFlashNotFound : KMError()
    data object BackFlashNotFound : KMError()
    data class DeviceNotFound(val descriptor: String) : KMError()
    data object InvalidNumber : KMError()
    data class NumberTooBig(val max: Int) : KMError()
    data class NumberTooSmall(val min: Int) : KMError()
    data object EmptyText : KMError()
    data object NoIncompatibleKeyboardsInstalled : KMError()
    data object NoMediaSessions : KMError()
    data object MediaActionUnsupported : KMError()
    data object BackupVersionTooNew : KMError()
    data object LauncherShortcutsNotSupported : KMError()

    data class AppNotFound(val packageName: String) : KMError()
    data class AppDisabled(val packageName: String) : KMError()
    data object AppShortcutCantBeOpened : KMError()
    data object InsufficientPermissionsToOpenAppShortcut : KMError()
    data object NoCompatibleImeEnabled : KMError()
    data object NoCompatibleImeChosen : KMError()

    data object CantShowImePickerInBackground : KMError()
    data object CantFindImeSettings : KMError()
    data object GestureStrokeCountTooHigh : KMError()
    data object GestureDurationTooHigh : KMError()

    data object FailedToFindAccessibilityNode : KMError()
    data class FailedToPerformAccessibilityGlobalAction(val action: Int) : KMError()
    data object FailedToDispatchGesture : KMError()

    data object CameraInUse : KMError()
    data object CameraDisconnected : KMError()
    data object CameraDisabled : KMError()
    data object MaxCamerasInUse : KMError()
    data object CameraError : KMError()
    data object CameraVariableFlashlightStrengthUnsupported : KMError()
    data object NightDisplayNotSupported : KMError()

    data class FailedToModifySystemSetting(val setting: String) : KMError()
    data object SwitchImeFailed : KMError()
    data object EnableImeFailed : KMError()
    data object NoAppToOpenUrl : KMError()
    data object NoAppToPhoneCall : KMError()
    data object NoAppToSendSms : KMError()
    data class SendSmsError(val resultCode: Int) : KMError()
    data object KeyMapperSmsRateLimit : KMError()

    data class NotAFile(val uri: String) : KMError()
    data class NotADirectory(val uri: String) : KMError()
    data object StoragePermissionDenied : KMError()
    data class CannotCreateFileInTarget(val uri: String) : KMError()
    data class SourceFileNotFound(val uri: String) : KMError()
    data class TargetFileNotFound(val uri: String) : KMError()
    data class TargetDirectoryNotFound(val uri: String) : KMError()
    data object UnknownIOError : KMError()
    data object FileOperationCancelled : KMError()
    data object TargetDirectoryMatchesSourceDirectory : KMError()
    data class NoSpaceLeftOnTarget(val uri: String) : KMError()
    data object NoFileName : KMError()
    data object InvalidBackup : KMError()

    data object EmptyJson : KMError()
    data object CantFindSoundFile : KMError()
    data class CorruptJsonFile(val reason: String) : KMError()

    data object ShizukuNotStarted : KMError()
    data object CantDetectKeyEventsInPhoneCall : KMError()

    /**
     * DPAD triggers require a Key Mapper keyboard to be selected.
     */
    data object DpadTriggerImeNotSelected : KMError()
    data object MalformedUrl : KMError()

    data object UiElementNotFound : KMError()
    data class KeyEventActionError(val baseError: KMError) : KMError()
    data class ShellCommandTimeout(val timeoutMillis: Long, val stdout: String? = null) : KMError()
}

inline fun <T> KMResult<T>.onSuccess(f: (T) -> Unit): KMResult<T> {
    if (this is Success) {
        f(this.value)
    }

    return this
}

inline fun <T, U> KMResult<T>.onFailure(f: (error: KMError) -> U): KMResult<T> {
    if (this is KMError) {
        f(this)
    }

    return this
}

inline infix fun <T, U> KMResult<T>.then(f: (T) -> KMResult<U>) = when (this) {
    is Success -> f(this.value)
    is KMError -> this
}

suspend infix fun <T, U> KMResult<T>.suspendThen(f: suspend (T) -> KMResult<U>) = when (this) {
    is Success -> f(this.value)
    is KMError -> this
}

inline infix fun <T> KMResult<T>.otherwise(f: (error: KMError) -> KMResult<T>) = when (this) {
    is Success -> this
    is KMError -> f(this)
}

inline fun <T, U> KMResult<T>.resolve(
    onSuccess: (value: T) -> U,
    onFailure: (error: KMError) -> U,
) = when (this) {
    is Success -> onSuccess(this.value)
    is KMError -> onFailure(this)
}

inline infix fun <T> KMResult<T>.valueIfFailure(f: (error: KMError) -> T): T = when (this) {
    is Success -> this.value
    is KMError -> f(this)
}

fun <T> KMResult<T>.errorOrNull(): KMError? {
    when (this) {
        is KMError -> return this
        else -> Unit
    }

    return null
}

fun <T> KMResult<T>.valueOrNull(): T? {
    when (this) {
        is Success -> return this.value
        else -> Unit
    }

    return null
}

val <T> KMResult<T>.isError: Boolean
    get() = this is KMError

val <T> KMResult<T>.isSuccess: Boolean
    get() = this is Success

fun <T, U> KMResult<T>.handle(onSuccess: (value: T) -> U, onError: (error: KMError) -> U): U =
    when (this) {
        is Success -> onSuccess(value)
        is KMError -> onError(this)
    }

fun <T> T.success() = Success(this)
