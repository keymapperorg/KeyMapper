package io.github.sds100.keymapper.base.promode

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import io.github.sds100.keymapper.base.BaseMainActivity
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.system.notifications.NotificationController
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.notifications.KMNotificationAction
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupController
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.notifications.NotificationAdapter
import io.github.sds100.keymapper.system.notifications.NotificationModel
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles auto starting the system bridge when Key Mapper is launched and when
 * the System Bridge is killed not due to the user.
 */
@RequiresApi(Build.VERSION_CODES.Q)
@Singleton
class SystemBridgeAutoStarter @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val suAdapter: SuAdapter,
    private val shizukuAdapter: ShizukuAdapter,
    private val connectionManager: SystemBridgeConnectionManager,
    private val setupController: SystemBridgeSetupController,
    private val preferences: PreferenceRepository,
    private val networkAdapter: NetworkAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val notificationAdapter: NotificationAdapter,
    private val resourceProvider: ResourceProvider
) : ResourceProvider by resourceProvider {
    private val isAutoStartEnabled: Flow<Boolean> =
        preferences.get(Keys.isProModeAutoStartEnabled)
            .map { it ?: PreferenceDefaults.PRO_MODE_AUTOSTART }

    private val isAdbAutoStartAllowed: Flow<Boolean> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            combine(
                permissionAdapter.isGrantedFlow(Permission.WRITE_SECURE_SETTINGS),
                networkAdapter.isWifiConnected
            ) { isWriteSecureSettingsGranted, isWifiConnected -> isWriteSecureSettingsGranted && isWifiConnected }
        } else {
            flowOf(false)
        }

    /**
     * This must only be called once in the application lifecycle
     */
    fun init() {
        coroutineScope.launch {
            // TODO autostart when system bridge disconnects. Create ConnectionState sealed class for system bridge that stores whether is due to user or not
            combine(
                isAutoStartEnabled,
                suAdapter.isRootGranted,
                shizukuAdapter.isStarted,
                isAdbAutoStartAllowed
            ) { isAutoStartEnabled, isRooted, isShizukuStarted, isAdbAutoStartAllowed ->

                // Do not listen to changes in the connection state to prevent
                // auto starting straight after it has stopped
                if (autoStart(
                        isAutoStartEnabled,
                        isRooted,
                        isShizukuStarted,
                        isAdbAutoStartAllowed
                    )
                ) return@combine

            }.collect()
        }

    }

    private suspend fun autoStart(
        isAutoStartEnabled: Boolean,
        isRooted: Boolean,
        isShizukuStarted: Boolean,
        isAdbAutoStartAllowed: Boolean
    ): Boolean {
        val isSystemBridgeConnected = connectionManager.isConnected.first()

        if (!isAutoStartEnabled || isSystemBridgeConnected) {
            return true
        }

        if (isRooted) {
            Timber.i("Auto starting system bridge with root")
            showAutoStartNotification(getString(R.string.pro_mode_setup_notification_auto_start_system_bridge_root_text))
            connectionManager.startWithRoot()
        } else if (isShizukuStarted) {
            Timber.i("Auto starting system bridge with Shizuku")
            showAutoStartNotification(getString(R.string.pro_mode_setup_notification_auto_start_system_bridge_shizuku_text))
            connectionManager.startWithShizuku()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && isAdbAutoStartAllowed) {
            autoStartWithAdb()
        } else {
            return true
        }

        // Wait 10 seconds for it to start, and if not then show failed notification.
        try {
            val isConnected = withTimeout(10000L) {
                connectionManager.isConnected.first { it }
            }

            if (isConnected) {
                showStartedNotification()
            }
        } catch (_: TimeoutCancellationException) {
            showAutoStartFailedNotification()
        } finally {
            dismissNotification()
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
    private suspend fun autoStartWithAdb() {
        Timber.i("Auto starting system bridge with ADB")
        showAutoStartNotification(getString(R.string.pro_mode_setup_notification_auto_start_system_bridge_adb_text))

        setupController.enableWirelessDebugging()

        if (setupController.isAdbPaired()) {
            setupController.startWithAdb()
        }
    }

    private fun showAutoStartNotification(text: String) {
        val model = NotificationModel(
            id = NotificationController.ID_SYSTEM_BRIDGE_STATUS,
            title = getString(R.string.pro_mode_setup_notification_auto_start_system_bridge_title),
            text = text,
            channel = NotificationController.CHANNEL_SETUP_ASSISTANT,
            icon = R.drawable.pro_mode,
            onGoing = true,
            showOnLockscreen = false
        )

        notificationAdapter.showNotification(model)
    }

    private fun showAutoStartFailedNotification() {
        val model = NotificationModel(
            id = NotificationController.ID_SYSTEM_BRIDGE_STATUS,
            title = getString(R.string.pro_mode_setup_notification_start_system_bridge_failed_title),
            text = getString(R.string.pro_mode_setup_notification_start_system_bridge_failed_text),
            channel = NotificationController.CHANNEL_SETUP_ASSISTANT,
            icon = R.drawable.pro_mode,
            onGoing = false,
            showOnLockscreen = false,
            onClickAction = KMNotificationAction.Activity.MainActivity(BaseMainActivity.ACTION_START_SYSTEM_BRIDGE)
        )

        notificationAdapter.showNotification(model)
    }

    private fun showStartedNotification() {
        val model = NotificationModel(
            id = NotificationController.ID_SYSTEM_BRIDGE_STATUS,
            title = getString(R.string.pro_mode_setup_notification_system_bridge_started_title),
            text = getString(R.string.pro_mode_setup_notification_system_bridge_started_text),
            channel = NotificationController.CHANNEL_SETUP_ASSISTANT,
            icon = R.drawable.pro_mode,
            onGoing = false,
            showOnLockscreen = false,
            autoCancel = true
        )

        notificationAdapter.showNotification(model)
    }

    private fun dismissNotification() {
        notificationAdapter.dismissNotification(NotificationController.ID_SYSTEM_BRIDGE_STATUS)
    }
}