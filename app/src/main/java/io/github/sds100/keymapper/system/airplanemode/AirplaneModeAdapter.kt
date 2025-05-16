package io.github.sds100.keymapper.system.airplanemode

import io.github.sds100.keymapper.common.result.Result

interface AirplaneModeAdapter {
    fun isEnabled(): Boolean
    fun enable(): Result<*>
    fun disable(): Result<*>
}
