package io.github.sds100.keymapper.base.promode

import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupController
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupStep
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    private val accessibilityServiceAdapter: AccessibilityServiceAdapter,
    private val networkAdapter: NetworkAdapter
) : SystemBridgeSetupUseCase {
    override val isWarningUnderstood: Flow<Boolean> =
        preferences.get(Keys.isProModeWarningUnderstood).map { it ?: false }

    override fun onUnderstoodWarning() {
        preferences.set(Keys.isProModeWarningUnderstood, true)
    }

    override val isSetupAssistantEnabled: Flow<Boolean> =
        preferences.get(Keys.isProModeInteractiveSetupAssistantEnabled).map {
            it ?: PreferenceDefaults.PRO_MODE_INTERACTIVE_SETUP_ASSISTANT
        }

    override fun toggleSetupAssistant() {
        preferences.update(Keys.isProModeInteractiveSetupAssistantEnabled) {
            if (it == null) {
                !PreferenceDefaults.PRO_MODE_INTERACTIVE_SETUP_ASSISTANT
            } else {
                !it
            }
        }
    }

    override val isSystemBridgeConnected: Flow<Boolean> = systemBridgeConnectionManager.isConnected

    @RequiresApi(Build.VERSION_CODES.R)
    override val nextSetupStep: Flow<SystemBridgeSetupStep> =
        isSystemBridgeConnected.flatMapLatest { isConnected ->
            if (isConnected) {
                flowOf(SystemBridgeSetupStep.STARTED)
            } else {
                combine(
                    accessibilityServiceAdapter.state,
                    permissionAdapter.isGrantedFlow(Permission.POST_NOTIFICATIONS),
                    systemBridgeSetupController.isDeveloperOptionsEnabled,
                    networkAdapter.isWifiConnected,
                    systemBridgeSetupController.isWirelessDebuggingEnabled,
                    ::getNextStep
                )
            }
        }

    @RequiresApi(Build.VERSION_CODES.R)
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

    override fun requestNotificationPermission() {
        permissionAdapter.request(Permission.POST_NOTIFICATIONS)
    }

    override fun stopSystemBridge() {
        systemBridgeConnectionManager.stopSystemBridge()
    }

    override fun enableAccessibilityService() {
        accessibilityServiceAdapter.start()
    }

    override fun enableDeveloperOptions() {
        systemBridgeSetupController.enableDeveloperOptions()
    }

    override fun connectWifiNetwork() {
        networkAdapter.connectWifiNetwork()
    }

    override fun enableWirelessDebugging() {
        systemBridgeSetupController.enableWirelessDebugging()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun pairWirelessAdb() {
        systemBridgeSetupController.launchPairingAssistant()
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

    override fun isInfoDismissed(): Boolean {
        return preferences.get(Keys.isProModeInfoDismissed).map { it ?: false }.firstBlocking()
    }

    override fun dismissInfo() {
        preferences.set(Keys.isProModeInfoDismissed, true)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun getNextStep(
        accessibilityServiceState: AccessibilityServiceState,
        isNotificationPermissionGranted: Boolean,
        isDeveloperOptionsEnabled: Boolean,
        isWifiConnected: Boolean,
        isWirelessDebuggingEnabled: Boolean
    ): SystemBridgeSetupStep {
        return when {
            accessibilityServiceState != AccessibilityServiceState.ENABLED -> SystemBridgeSetupStep.ACCESSIBILITY_SERVICE
            !isNotificationPermissionGranted -> SystemBridgeSetupStep.NOTIFICATION_PERMISSION
            !isDeveloperOptionsEnabled -> SystemBridgeSetupStep.DEVELOPER_OPTIONS
            !isWifiConnected -> SystemBridgeSetupStep.WIFI_NETWORK
            !isWirelessDebuggingEnabled -> SystemBridgeSetupStep.WIRELESS_DEBUGGING
            isWirelessDebuggingEnabled && !systemBridgeSetupController.isAdbPaired() -> SystemBridgeSetupStep.ADB_PAIRING
            else -> SystemBridgeSetupStep.START_SERVICE
        }
    }

}

interface SystemBridgeSetupUseCase {
    val isWarningUnderstood: Flow<Boolean>
    fun onUnderstoodWarning()

    fun isInfoDismissed(): Boolean
    fun dismissInfo()

    val isSetupAssistantEnabled: Flow<Boolean>
    fun toggleSetupAssistant()

    val isSystemBridgeConnected: Flow<Boolean>
    val nextSetupStep: Flow<SystemBridgeSetupStep>
    val setupProgress: Flow<Float>

    val isRootGranted: Flow<Boolean>

    val shizukuSetupState: Flow<ShizukuSetupState>
    fun openShizukuApp()
    fun requestShizukuPermission()
    fun requestNotificationPermission()

    fun stopSystemBridge()
    fun enableAccessibilityService()
    fun enableDeveloperOptions()
    fun connectWifiNetwork()
    fun enableWirelessDebugging()
    fun pairWirelessAdb()
    fun startSystemBridgeWithRoot()
    fun startSystemBridgeWithShizuku()
    fun startSystemBridgeWithAdb()
}
