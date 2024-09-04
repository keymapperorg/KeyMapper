package io.github.sds100.keymapper.system.airplanemode

import io.github.sds100.keymapper.util.Result

/**
 * Created by sds100 on 24/04/2021.
 */
interface AirplaneModeAdapter {
    fun isEnabled(): Boolean
    fun enable(): Result<*>
    fun disable(): Result<*>
}
