package io.github.sds100.keymapper.system.accessibility

import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import kotlinx.coroutines.flow.Flow

class ControlAccessibilityServiceUseCaseImpl(
    private val adapter: ServiceAdapter,
    private val permissionAdapter: PermissionAdapter,
) : ControlAccessibilityServiceUseCase {
    override val serviceState: Flow<ServiceState> = adapter.state

    /**
     * @return true if the service could be started of if the accessibility settings could be
     * opened. False if no way can be found to start the service.
     */
    override fun startService(): Boolean = adapter.start()

    /**
     * @return true if the service could be restart of if the accessibility settings could be
     * opened. False if no way can be found to start the service.
     */
    override fun restartService(): Boolean = adapter.restart()

    override fun stopService() {
        adapter.stop()
    }

    /**
     * @return whether the user must manually start/stop the service.
     */
    override fun isUserInteractionRequired(): Boolean {
        return !permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS)
    }
}

interface ControlAccessibilityServiceUseCase {
    val serviceState: Flow<ServiceState>
    fun startService(): Boolean
    fun restartService(): Boolean
    fun stopService()
    fun isUserInteractionRequired(): Boolean
}
