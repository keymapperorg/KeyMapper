package io.github.sds100.keymapper.base.promode

import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.base.BaseMainActivity
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.system.notifications.NotificationController
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.notifications.KMNotificationAction
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupController
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.notifications.NotificationAdapter
import io.github.sds100.keymapper.system.notifications.NotificationModel
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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

    enum class AutoStartType {
        ADB, SHIZUKU, ROOT
    }

    private val isAdbAutoStartAllowed: Flow<Boolean> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            combine(
                permissionAdapter.isGrantedFlow(Permission.WRITE_SECURE_SETTINGS),
                networkAdapter.isWifiConnected
            ) { isWriteSecureSettingsGranted, isWifiConnected -> isWriteSecureSettingsGranted && isWifiConnected }
        } else {
            flowOf(false)
        }

    private val autoStartFlow: Flow<AutoStartType?> =
        isAutoStartEnabled.flatMapLatest { autoStartEnabled ->
            if (autoStartEnabled) {
                combine(
                    suAdapter.isRootGranted,
                    shizukuAdapter.isStarted,
                    isAdbAutoStartAllowed.distinctUntilChanged(),
                    connectionManager.connectionState,
                    ::getAutoStartType
                )
            } else {
                flowOf(null)
            }
        }

    private var lastAutoStartTime: Long? = null

    private fun getAutoStartType(
        isRooted: Boolean,
        isShizukuStarted: Boolean,
        isAdbAutoStartAllowed: Boolean,
        connectionState: SystemBridgeConnectionState,
    ): AutoStartType? {
        // Do not autostart if it is connected or it was killed from the user
        if (connectionState !is SystemBridgeConnectionState.Disconnected || connectionState.isExpected) {
            return null
        }

        // Do not autostart if the system bridge was killed less than a minute after.
        // This prevents infinite loops happening.
        lastAutoStartTime?.let { lastAutoStartTime ->
            if (connectionState.time - lastAutoStartTime < 60000) {
                Timber.w("Not auto starting the system bridge because it was last auto started less than one minute ago")
                return null
            }
        }

        return when {
            isRooted -> AutoStartType.ROOT
            isShizukuStarted -> AutoStartType.SHIZUKU
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && isAdbAutoStartAllowed -> AutoStartType.ADB
            else -> null
        }
    }

    /**
     * This must only be called once in the application lifecycle
     */
    @OptIn(FlowPreview::class)
    fun init() {
        coroutineScope.launch {
            autoStartFlow
                .distinctUntilChanged() // Must come before the filterNotNull
                .filterNotNull()
                .collectLatest { type ->
                    autoStart(type)
                }
        }
    }

    private suspend fun autoStart(type: AutoStartType) {
        lastAutoStartTime = SystemClock.elapsedRealtime()

        when (type) {
            AutoStartType.ADB -> {
                Timber.i("Auto starting system bridge with ADB")
                showAutoStartNotification(getString(R.string.pro_mode_setup_notification_auto_start_system_bridge_adb_text))

                setupController.autoStartWithAdb()
            }

            AutoStartType.SHIZUKU -> {
                Timber.i("Auto starting system bridge with Shizuku")
                showAutoStartNotification(getString(R.string.pro_mode_setup_notification_auto_start_system_bridge_shizuku_text))
                connectionManager.startWithShizuku()
            }

            AutoStartType.ROOT -> {
                Timber.i("Auto starting system bridge with root")
                showAutoStartNotification(getString(R.string.pro_mode_setup_notification_auto_start_system_bridge_root_text))
                connectionManager.startWithRoot()
            }
        }

        // Wait 30 seconds for it to start, and if not then show failed notification.
        try {
            withTimeout(30000L) {
                connectionManager.connectionState.first { it is SystemBridgeConnectionState.Connected }
            }
        } catch (_: TimeoutCancellationException) {
            showAutoStartFailedNotification()
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
            onClickAction = KMNotificationAction.Activity.MainActivity(BaseMainActivity.ACTION_START_SYSTEM_BRIDGE),
        )

        notificationAdapter.showNotification(model)
    }
}