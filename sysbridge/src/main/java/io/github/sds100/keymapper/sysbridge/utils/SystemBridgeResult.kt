package io.github.sds100.keymapper.sysbridge.utils

import io.github.sds100.keymapper.common.utils.KMError

sealed class SystemBridgeError : KMError() {
    data object Disconnected : SystemBridgeError()
}