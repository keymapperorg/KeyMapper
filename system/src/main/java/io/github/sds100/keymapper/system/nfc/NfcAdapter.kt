package io.github.sds100.keymapper.system.nfc

import io.github.sds100.keymapper.common.utils.Result


interface NfcAdapter {
    fun isEnabled(): Boolean
    fun enable(): Result<*>
    fun disable(): Result<*>
}
