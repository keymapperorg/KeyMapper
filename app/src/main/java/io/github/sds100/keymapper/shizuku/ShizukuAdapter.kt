package io.github.sds100.keymapper.shizuku

import kotlinx.coroutines.flow.StateFlow

/**
 * Created by sds100 on 20/07/2021.
 */
interface ShizukuAdapter {
    val isInstalled: StateFlow<Boolean>
    val isStarted: StateFlow<Boolean>
    fun openShizukuApp()
}
