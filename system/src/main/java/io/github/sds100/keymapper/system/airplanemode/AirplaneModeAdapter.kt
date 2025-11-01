package io.github.sds100.keymapper.system.airplanemode

import io.github.sds100.keymapper.common.utils.KMResult

interface AirplaneModeAdapter {
    fun isEnabled(): Boolean
    suspend fun enable(): KMResult<*>
    suspend fun disable(): KMResult<*>
}
