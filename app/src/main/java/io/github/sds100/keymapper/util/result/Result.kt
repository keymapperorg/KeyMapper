package io.github.sds100.keymapper.util.result

import android.content.Context
import androidx.annotation.IntDef

/**
 * Created by sds100 on 26/02/2020.
 */

@IntDef(value = [
    ERROR_CODE_NO_ACTION_DATA,
    ERROR_CODE_ACTION_IS_NULL,
    ERROR_CODE_APP_DISABLED,
    ERROR_CODE_APP_UNINSTALLED,
    ERROR_CODE_PERMISSION_DENIED,
    ERROR_CODE_SHORTCUT_NOT_FOUND,
    ERROR_CODE_SYSTEM_ACTION_NOT_FOUND,
    ERROR_CODE_FEATURE_NOT_AVAILABLE,
    ERROR_CODE_SDK_VERSION_TOO_LOW,
    ERROR_CODE_ACTION_EXTRA_NOT_FOUND,
    ERROR_CODE_NULL]
)
annotation class ErrorCode

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
const val ERROR_CODE_FLAG_NOT_FOUND = 11
const val ERROR_CODE_GOOGLE_APP_NOT_INSTALLED = 12
const val ERROR_CODE_NULL = 13

/**
 * Inspired from @antonyharfield great example!
 */

sealed class Result<out T>

data class Success<T>(val value: T) : Result<T>()
abstract class Failure(val errorMessage: String) : Result<Nothing>() {
    open fun recover(ctx: Context) {}
}

fun <T> Result<T>.onSuccess(f: (T) -> Unit): Result<T> {
    if (this is Success) {
        f(this.value)
    }

    return this
}

fun <T, U> Result<T>.onFailure(f: (errorMessage: String) -> U): Result<T> {
    if (this is Failure) {
        f(this.errorMessage)
    }

    return this
}

infix fun <T, U> Result<T>.then(f: (T) -> Result<U>): Result<U> {
    return when (this) {
        is Success -> f(this.value)
        is Failure -> this
    }
}

fun <T> Result<T>.errorMessageOrNull(): String? {
    when (this) {
        is Failure -> return this.errorMessage
    }

    return null
}

infix fun <T> Result<T>.otherwise(f: (failure: Failure) -> Unit) =
    if (this is Failure) f(this) else Unit