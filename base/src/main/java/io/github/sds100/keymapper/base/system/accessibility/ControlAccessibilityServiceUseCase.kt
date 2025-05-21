package io.github.sds100.keymapper.base.system.accessibility

import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Singleton
class ControlAccessibilityServiceUseCaseImpl(
    private val adapter: AccessibilityServiceAdapter,
    private val permissionAdapter: PermissionAdapter,
) : ControlAccessibilityServiceUseCase {
    override val serviceState: Flow<AccessibilityServiceState> = adapter.state

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
    val serviceState: Flow<AccessibilityServiceState>
    fun startService(): Boolean
    fun restartService(): Boolean
    fun stopService()
    fun isUserInteractionRequired(): Boolean
}
