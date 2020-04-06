package io.github.sds100.keymapper.util.result

import androidx.fragment.app.FragmentActivity

/**
 * Created by sds100 on 26/02/2020.
 */

/**
 * Inspired from @antonyharfield great example!
 */

sealed class Result<out T>

data class Success<T>(val value: T) : Result<T>()
abstract class Failure(val fullMessage: String, val briefMessage: String = fullMessage) : Result<Nothing>()

abstract class RecoverableFailure(
    fullMessage: String,
    briefMessage: String = fullMessage
) : Failure(fullMessage, briefMessage) {
    abstract suspend fun recover(activity: FragmentActivity, onSuccess: () -> Unit = {})
}

inline fun <T> Result<T>.onSuccess(f: (T) -> Unit): Result<T> {
    if (this is Success) {
        f(this.value)
    }

    return this
}

inline fun <T, U> Result<T>.onFailure(f: (failure: Failure) -> U): Result<T> {
    if (this is Failure) {
        f(this)
    }

    return this
}

infix fun <T, U> Result<T>.then(f: (T) -> Result<U>) =
    when (this) {
        is Success -> f(this.value)
        is Failure -> this
    }

infix fun <T> Result<T>.otherwise(f: (failure: Failure) -> Result<T>) =
    when (this) {
        is Success -> this
        is Failure -> f(this)
    }

fun <T> Result<T>.errorMessageOrNull(): String? {
    when (this) {
        is Failure -> return this.fullMessage
    }

    return null
}

fun <T> Result<T>.failureOrNull(): Failure? {
    when (this) {
        is Failure -> return this
    }

    return null
}

fun <T> Result<T>.valueOrNull(): T? {
    when (this) {
        is Success -> return this.value
    }

    return null
}

fun <T, U> Result<T>.handle(onSuccess: (value: T) -> U, onFailure: (failure: Failure) -> U): U {
    return when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(this)
    }
}