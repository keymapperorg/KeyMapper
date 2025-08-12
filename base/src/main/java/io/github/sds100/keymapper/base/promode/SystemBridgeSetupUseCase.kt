package io.github.sds100.keymapper.base.promode

import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupController
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupStep
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject


@RequiresApi(Build.VERSION_CODES.Q)
@ViewModelScoped
class SystemBridgeSetupUseCaseImpl @Inject constructor(
    private val preferences: PreferenceRepository,
    private val suAdapter: SuAdapter,
    private val systemBridgeSetupController: SystemBridgeSetupController,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    private val shizukuAdapter: ShizukuAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val accessibilityServiceAdapter: AccessibilityServiceAdapter
) : SystemBridgeSetupUseCase {
    override val isWarningUnderstood: Flow<Boolean> =
        preferences.get(Keys.isProModeWarningUnderstood).map { it ?: false }

    override fun onUnderstoodWarning() {
        preferences.set(Keys.isProModeWarningUnderstood, true)
    }

    override val isSystemBridgeConnected: Flow<Boolean> = systemBridgeConnectionManager.isConnected

    override val nextSetupStep: Flow<SystemBridgeSetupStep> =
        systemBridgeSetupController.nextSetupStep

    override val setupProgress: Flow<Float> = nextSetupStep.map { step ->
        step.stepIndex.toFloat() / SystemBridgeSetupStep.entries.size
    }

    override val isRootGranted: Flow<Boolean> = suAdapter.isRootGranted

    override val shizukuSetupState: Flow<ShizukuSetupState> = combine(
        shizukuAdapter.isInstalled,
        shizukuAdapter.isStarted,
        permissionAdapter.isGrantedFlow(Permission.SHIZUKU)
    ) { isInstalled, isStarted, isPermissionGranted ->
        when {
            isPermissionGranted -> ShizukuSetupState.PERMISSION_GRANTED
            isStarted -> ShizukuSetupState.STARTED
            isInstalled -> ShizukuSetupState.INSTALLED
            else -> ShizukuSetupState.NOT_FOUND
        }
    }

    override fun openShizukuApp() {
        shizukuAdapter.openShizukuApp()
    }

    override fun requestShizukuPermission() {
        permissionAdapter.request(Permission.SHIZUKU)
    }

    override fun stopSystemBridge() {
        systemBridgeConnectionManager.stopSystemBridge()
    }

    override fun enableAccessibilityService() {
        accessibilityServiceAdapter.start()
    }

    override fun openDeveloperOptions() {
        TODO("Not yet implemented")
    }

    override fun connectWifiNetwork() {
        TODO("Not yet implemented")
    }

    override fun enableWirelessDebugging() {
        TODO("Not yet implemented")
    }

    override fun pairAdb() {
        TODO("Not yet implemented")
    }

    override fun startSystemBridgeWithRoot() {
        systemBridgeSetupController.startWithRoot()
    }

    override fun startSystemBridgeWithShizuku() {
        systemBridgeSetupController.startWithShizuku()
    }

    override fun startSystemBridgeWithAdb() {
        systemBridgeSetupController.startWithAdb()
    }
}

interface SystemBridgeSetupUseCase {
    val isWarningUnderstood: Flow<Boolean>
    fun onUnderstoodWarning()

    val isSystemBridgeConnected: Flow<Boolean>
    val nextSetupStep: Flow<SystemBridgeSetupStep>
    val setupProgress: Flow<Float>

    val isRootGranted: Flow<Boolean>

    val shizukuSetupState: Flow<ShizukuSetupState>
    fun openShizukuApp()
    fun requestShizukuPermission()

    fun stopSystemBridge()
    fun enableAccessibilityService()
    fun openDeveloperOptions()
    fun connectWifiNetwork()
    fun enableWirelessDebugging()
    fun pairAdb()
    fun startSystemBridgeWithRoot()
    fun startSystemBridgeWithShizuku()
    fun startSystemBridgeWithAdb()
}
