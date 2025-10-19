package io.github.sds100.keymapper.base.system.accessibility

import android.os.Build
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControlAccessibilityServiceUseCaseImpl @Inject constructor(
    private val adapter: AccessibilityServiceAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val packageManagerAdapter: PackageManagerAdapter
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

    override fun acknowledgeCrashed() {
        adapter.acknowledgeCrashed()
    }

    /**
     * @return whether the user must manually start/stop the service.
     */
    override fun isUserInteractionRequired(): Boolean {
        return !permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS)
    }

    override fun isRestrictedSetting(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // If the app is installed outside of Google Play then Android disables
            // the accessibility service until restricted settings are enabled.
            packageManagerAdapter.getInstallSourcePackageName() != "com.android.vending"
        } else {
            false
        }
    }
}

interface ControlAccessibilityServiceUseCase {
    val serviceState: Flow<AccessibilityServiceState>
    fun startService(): Boolean
    fun restartService(): Boolean
    fun stopService()
    fun acknowledgeCrashed()

    fun isUserInteractionRequired(): Boolean
    fun isRestrictedSetting(): Boolean
}
