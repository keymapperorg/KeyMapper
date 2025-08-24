package io.github.sds100.keymapper.sysbridge.adb

import io.github.sds100.keymapper.common.utils.KMError

sealed class AdbError : KMError() {
    data object PairingError : AdbError()
    data object ServerNotFound : AdbError()
    data object KeyCreationError : AdbError()
    data object ConnectionError : AdbError()
    data object SslHandshakeError : AdbError()
    data class Unknown(val exception: kotlin.Exception) : AdbError()
}