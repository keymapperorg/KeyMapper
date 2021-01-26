package io.github.sds100.keymapper.util.result

/**
 * Created by sds100 on 26/02/2020.
 */

/**
 * Inspired from @antonyharfield great example!
 */

sealed class Result<out T>

data class Success<T>(val value: T) : Result<T>()

abstract class Failure : Result<Nothing>()
abstract class RecoverableFailure : Failure()

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

suspend infix fun <T, U> Result<T>.suspendThen(f: suspend (T) -> Result<U>) =
    when (this) {
        is Success -> f(this.value)
        is Failure -> this
    }

infix fun <T> Result<T>.otherwise(f: (failure: Failure) -> Result<T>) =
    when (this) {
        is Success -> this
        is Failure -> f(this)
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

val <T> Result<T>.isFailure: Boolean
    get() = this is Failure

val <T> Result<T>.isSuccess: Boolean
    get() = this is Success

fun <T, U> Result<T>.handle(onSuccess: (value: T) -> U, onFailure: (failure: Failure) -> U): U {
    return when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(this)
    }
}

suspend fun <T, U> Result<T>.handleAsync(onSuccess: suspend (value: T) -> U, onFailure: suspend (failure: Failure) -> U): U {
    return when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(this)
    }
}
