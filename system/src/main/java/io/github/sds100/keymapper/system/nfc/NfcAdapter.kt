package io.github.sds100.keymapper.system.nfc

import io.github.sds100.keymapper.common.utils.KMResult

interface NfcAdapter {
    fun isEnabled(): Boolean
    fun enable(): KMResult<*>
    fun disable(): KMResult<*>
}
