package io.github.sds100.keymapper.system.power

import kotlinx.coroutines.flow.StateFlow

/**
 * Created by sds100 on 21/05/2022.
 */
interface PowerAdapter {
    val isCharging: StateFlow<Boolean>
}
