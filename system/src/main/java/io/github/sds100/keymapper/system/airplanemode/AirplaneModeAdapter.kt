package io.github.sds100.keymapper.system.airplanemode

import io.github.sds100.keymapper.common.utils.KMResult

interface AirplaneModeAdapter {
    fun isEnabled(): Boolean
    fun enable(): KMResult<*>
    fun disable(): KMResult<*>
}
