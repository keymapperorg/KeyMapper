package io.github.sds100.keymapper.shizuku

import kotlinx.coroutines.flow.StateFlow


interface ShizukuAdapter {
    val isInstalled: StateFlow<Boolean>
    val isStarted: StateFlow<Boolean>
    fun openShizukuApp()
}
