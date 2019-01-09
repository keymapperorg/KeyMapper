package io.github.sds100.keymapper

import io.github.sds100.keymapper.Utils.ErrorCode

/**
 * Created by sds100 on 31/12/2018.
 */

data class Result<T>(val data: T? = null, val errorResult: ErrorResult? = null) {

    constructor(data: T) : this(data, null)
    constructor(error: ErrorResult) : this(null, error)

    val isSuccess
        get() = data != null

    val isFailure
        get() = errorResult != null

    /**
     * @return a string representation of the [data] and whether it is a success or a failure.
     */
    override fun toString(): String {
        return if (isSuccess) {
            "Success(${data.toString()})"
        } else {
            "Failure(${data.toString()})"
        }
    }
}

/**
 * Get the result from any object. If the object isn't null, a [Result] is returned with the object as the [Result.data].
 * If the object is null, the [Result.data] will be null and the specified [errorCode] and [errorDescription] will be
 * used.
 */
fun <T> T?.createResult(@ErrorCode errorCode: Int, errorDescription: String? = null): Result<T> {
    return if (this == null) {
        Result(ErrorResult(errorCode, errorDescription))
    } else {
        Result(this)
    }
}

fun <T, E> Result<T>.handle(onSuccess: (data: T) -> E, onFailure: (errorResult: ErrorResult) -> E): E {
    return if (this.isSuccess) {
        onSuccess(data!!)
    } else {
        onFailure(errorResult!!)
    }
}

/**
 * @return Performs the given action if the result is successful.
 */
fun <T, E> Result<T>.onSuccess(action: (data: T) -> E): E? {
    if (this.isSuccess) return action(this.data!!)

    return null
}


/**
 * @return Performs the given action if the result is a failure.
 */
fun <T, E> Result<T>.onFailure(action: (errorResult: ErrorResult) -> E): E? {
    if (this.isFailure) return action(errorResult!!)

    return null
}
