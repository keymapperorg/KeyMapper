package io.github.sds100.keymapper.system.accessibility

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Created by sds100 on 16/04/2021.
 */

class ControlAccessibilityServiceUseCaseImpl @Inject constructor(
    private val adapter: AccessibilityServiceAdapter
) : ControlAccessibilityServiceUseCase {
    override val serviceState: Flow<ServiceState> = adapter.state

    /**
     * @return true if the service could be started of if the accessibility settings could be
     * opened. False if no way can be found to start the service.
     */
    override fun startService(): Boolean {
        return adapter.start()
    }

    /**
     * @return true if the service could be restart of if the accessibility settings could be
     * opened. False if no way can be found to start the service.
     */
    override fun restartService(): Boolean {
        return adapter.restart()
    }

    override fun stopService() {
        adapter.stop()
    }
}

interface ControlAccessibilityServiceUseCase {
    val serviceState: Flow<ServiceState>
    fun startService(): Boolean
    fun restartService(): Boolean
    fun stopService()
}