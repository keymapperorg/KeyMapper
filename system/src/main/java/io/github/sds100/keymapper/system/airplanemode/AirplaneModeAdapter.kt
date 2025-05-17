package io.github.sds100.keymapper.system.airplanemode

import io.github.sds100.keymapper.common.utils.Result

interface AirplaneModeAdapter {
    fun isEnabled(): Boolean
    fun enable(): Result<*>
    fun disable(): Result<*>
}
