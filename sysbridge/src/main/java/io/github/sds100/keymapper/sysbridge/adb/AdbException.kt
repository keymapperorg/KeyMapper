package io.github.sds100.keymapper.sysbridge.adb

@Suppress("NOTHING_TO_INLINE")
internal inline fun adbError(message: Any): Nothing = throw AdbException(message.toString())

internal open class AdbException : Exception {

    constructor(message: String, cause: Throwable?) : super(message, cause)
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)
    constructor()
}

internal class AdbInvalidPairingCodeException : AdbException()

internal class AdbKeyException(cause: Throwable) : AdbException(cause)
