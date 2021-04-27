package io.github.sds100.keymapper.system.phone

import io.github.sds100.keymapper.util.Result

/**
 * Created by sds100 on 21/04/2021.
 */
interface PhoneAdapter {
    fun startCall(number: String):Result<*>
}