package io.github.sds100.keymapper.base.promode

import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupController
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupStep
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@RequiresApi(Constants.SYSTEM_BRIDGE_MIN_API)
@ViewModelScoped
class SystemBridgeSetupUseCaseImpl @Inject constructor(
    private val preferences: PreferenceRepository,
    private val suAdapter: SuAdapter,
    private val systemBridgeSetupController: SystemBridgeSetupController,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    private val shizukuAdapter: ShizukuAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val accessibilityServiceAdapter: AccessibilityServiceAdapter,
    private val networkAdapter: NetworkAdapter,
) : SystemBridgeSetupUseCase {

    companion object {
        /**
         * This is specified in android/hardware/usb/UsbManager.java and called
         * FUNCTION_NONE in that file.
         */
        private const val USB_FUNCTION_NONE = 0
    }

    override val isWarningUnderstood: Flow<Boolean> =
        preferences.get(Keys.isProModeWarningUnderstood).map { it ?: false }

    private val isAdbAutoStartAllowed: Flow<Boolean> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            combine(
                permissionAdapter.isGrantedFlow(Permission.WRITE_SECURE_SETTINGS),
                networkAdapter.isWifiConnected,
            ) { isWriteSecureSettingsGranted, isWifiConnected ->
                isWriteSecureSettingsGranted &&
                    isWifiConnected &&
                    systemBridgeSetupController.isAdbPaired()
            }.flowOn(Dispatchers.IO)
        } else {
            flowOf(false)
        }

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

    override val isSystemBridgeConnected: Flow<Boolean> =
        systemBridgeConnectionManager.connectionState
            .map { it is SystemBridgeConnectionState.Connected }

    override val isNotificationPermissionGranted: Flow<Boolean> =
        permissionAdapter.isGrantedFlow(Permission.POST_NOTIFICATIONS)

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.R)
    override val nextSetupStep: Flow<SystemBridgeSetupStep> =
        isSystemBridgeConnected.flatMapLatest { isConnected ->
            if (isConnected) {
                flowOf(SystemBridgeSetupStep.STARTED)
            } else {
                isAdbAutoStartAllowed.flatMapLatest { isAdbAutoStartAllowed ->
                    if (isAdbAutoStartAllowed) {
                        flowOf(SystemBridgeSetupStep.START_SERVICE)
                    } else {
                        combine(
                            accessibilityServiceAdapter.state,
                            isNotificationPermissionGranted,
                            systemBridgeSetupController.isDeveloperOptionsEnabled,
                            networkAdapter.isWifiConnected,
                            systemBridgeSetupController.isWirelessDebuggingEnabled,
                            ::getNextStep,
                        )
                    }
                }
            }
        }

    override val isRootGranted: Flow<Boolean> = suAdapter.isRootGranted

    override val shizukuSetupState: Flow<ShizukuSetupState> = combine(
        shizukuAdapter.isInstalled,
        shizukuAdapter.isStarted,
        permissionAdapter.isGrantedFlow(Permission.SHIZUKU),
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
        preferences.set(Keys.isSystemBridgeEmergencyKilled, false)
        systemBridgeSetupController.startWithRoot()
    }

    override fun startSystemBridgeWithShizuku() {
        preferences.set(Keys.isSystemBridgeEmergencyKilled, false)
        systemBridgeSetupController.startWithShizuku()
    }

    override suspend fun startSystemBridgeWithAdb() {
        preferences.set(Keys.isSystemBridgeEmergencyKilled, false)
        if (isAdbAutoStartAllowed.first()) {
            systemBridgeSetupController.autoStartWithAdb()
        } else {
            systemBridgeSetupController.startWithAdb()
        }
    }

    override fun isInfoDismissed(): Boolean {
        return preferences.get(Keys.isProModeInfoDismissed).map { it ?: false }.firstBlocking()
    }

    override fun dismissInfo() {
        preferences.set(Keys.isProModeInfoDismissed, true)
    }

    override val isAutoStartBootEnabled: Flow<Boolean> =
        preferences.get(Keys.isProModeAutoStartBootEnabled)
            .map { it ?: PreferenceDefaults.PRO_MODE_AUTOSTART_BOOT }

    override fun toggleAutoStartBoot() {
        preferences.update(Keys.isProModeAutoStartBootEnabled) {
            !(it ?: PreferenceDefaults.PRO_MODE_AUTOSTART_BOOT)
        }
    }

    /**
     * This applies to older Android versions (tested on Android 11 and 13).
     * Having the "Default USB Configuration" in developer options set to something other
     * than "No data transfer" causes the system bridge (ADB) process to be killed when the device
     * is locked. Not 100% sure what the mechanic is but it can be reproduced by pressing the
     * power button. On Android 15 this is not the case because it seems like turning on
     * wireless debugging or ADB resets the setting to "No data transfer".
     */
    override fun isCompatibleUsbModeSelected(): KMResult<Boolean> {
        return systemBridgeConnectionManager
            .run { systemBridge ->
                // The USB setting does not matter if the system bridge is running as root
                // because it doesn't rely on the ADB process.
                systemBridge.processUid == Process.SHELL_UID &&
                    systemBridge.usbScreenUnlockedFunctions.toInt() == 0
            }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getNextStep(
        accessibilityServiceState: AccessibilityServiceState,
        isNotificationPermissionGranted: Boolean,
        isDeveloperOptionsEnabled: Boolean,
        isWifiConnected: Boolean,
        isWirelessDebuggingEnabled: Boolean,
    ): SystemBridgeSetupStep {
        return when {
            accessibilityServiceState != AccessibilityServiceState.ENABLED ->
                SystemBridgeSetupStep.ACCESSIBILITY_SERVICE
            !isNotificationPermissionGranted -> SystemBridgeSetupStep.NOTIFICATION_PERMISSION
            !isDeveloperOptionsEnabled -> SystemBridgeSetupStep.DEVELOPER_OPTIONS
            !isWifiConnected -> SystemBridgeSetupStep.WIFI_NETWORK
            !isWirelessDebuggingEnabled -> SystemBridgeSetupStep.WIRELESS_DEBUGGING
            isWirelessDebuggingEnabled -> SystemBridgeSetupStep.ADB_PAIRING
            else -> SystemBridgeSetupStep.START_SERVICE
        }
    }
}

interface SystemBridgeSetupUseCase {
    val isWarningUnderstood: Flow<Boolean>
    fun onUnderstoodWarning()

    fun isInfoDismissed(): Boolean
    fun dismissInfo()

    val isAutoStartBootEnabled: Flow<Boolean>
    fun toggleAutoStartBoot()

    val isSetupAssistantEnabled: Flow<Boolean>
    fun toggleSetupAssistant()

    val isSystemBridgeConnected: Flow<Boolean>
    val nextSetupStep: Flow<SystemBridgeSetupStep>

    val isRootGranted: Flow<Boolean>

    val shizukuSetupState: Flow<ShizukuSetupState>
    fun openShizukuApp()
    fun requestShizukuPermission()

    val isNotificationPermissionGranted: Flow<Boolean>
    fun requestNotificationPermission()

    fun stopSystemBridge()
    fun enableAccessibilityService()
    fun enableDeveloperOptions()
    fun connectWifiNetwork()
    fun enableWirelessDebugging()
    fun pairWirelessAdb()
    fun startSystemBridgeWithRoot()
    fun startSystemBridgeWithShizuku()
    suspend fun startSystemBridgeWithAdb()

    fun isCompatibleUsbModeSelected(): KMResult<Boolean>
}
