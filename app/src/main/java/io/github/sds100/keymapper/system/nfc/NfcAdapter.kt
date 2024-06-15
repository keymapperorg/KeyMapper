package io.github.sds100.keymapper.system.nfc

import io.github.sds100.keymapper.util.Result

/**
 * Created by sds100 on 24/04/2021.
 */
interface NfcAdapter {
    fun isEnabled(): Boolean
    fun enable(): Result<*>
    fun disable(): Result<*>
}
