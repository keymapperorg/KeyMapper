package io.github.sds100.keymapper.system.accessibility

import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 16/04/2021.
 */

class ControlAccessibilityServiceUseCaseImpl(
    private val adapter: ServiceAdapter
) : ControlAccessibilityServiceUseCase {
    override val state: Flow<ServiceState> = adapter.state

    override fun enable() {
        adapter.enableService()
    }

    override fun restart() {
        adapter.restartService()
    }

    override fun disable() {
        adapter.disableService()
    }
}

interface ControlAccessibilityServiceUseCase {
    val state: Flow<ServiceState>
    fun enable()
    fun restart()
    fun disable()
}