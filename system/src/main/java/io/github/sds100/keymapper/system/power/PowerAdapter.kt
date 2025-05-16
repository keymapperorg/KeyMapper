package io.github.sds100.keymapper.system.power

import kotlinx.coroutines.flow.StateFlow


interface PowerAdapter {
    val isCharging: StateFlow<Boolean>
}
