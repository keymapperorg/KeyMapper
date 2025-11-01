package io.github.sds100.keymapper.base.promode

import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import io.github.sds100.keymapper.base.BaseMainActivity
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.system.notifications.NotificationController.Companion.CHANNEL_SETUP_ASSISTANT
import io.github.sds100.keymapper.base.system.notifications.NotificationController.Companion.ID_SYSTEM_BRIDGE_STATUS
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.notifications.KMNotificationAction
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.BuildConfig
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
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

/**
 * This class handles auto starting the system bridge when Key Mapper is launched and when
 * the System Bridge is killed not due to the user.
 */
@RequiresApi(Constants.SYSTEM_BRIDGE_MIN_API)
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
    private val resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider {
    enum class AutoStartType {
        ADB,
        SHIZUKU,
        ROOT,
    }

    // Use flatMapLatest so that any calls to ADB are only done if strictly necessary.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val autoStartTypeFlow: Flow<AutoStartType?> =
        suAdapter.isRootGranted.flatMapLatest { isRooted ->
            if (isRooted) {
                flowOf(AutoStartType.ROOT)
            } else {
                shizukuAdapter.isStarted.flatMapLatest { isShizukuStarted ->
                    if (isShizukuStarted) {
                        flowOf(AutoStartType.SHIZUKU)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val isAdbAutoStartAllowed = combine(
                            permissionAdapter.isGrantedFlow(Permission.WRITE_SECURE_SETTINGS),
                            networkAdapter.isWifiConnected,
                        ) { isWriteSecureSettingsGranted, isWifiConnected ->
                            isWriteSecureSettingsGranted &&
                                isWifiConnected &&
                                setupController.isAdbPaired()
                        }

                        isAdbAutoStartAllowed.distinctUntilChanged()
                            .map { isAdbAutoStartAllowed ->
                                if (isAdbAutoStartAllowed) AutoStartType.ADB else null
                            }.filterNotNull()
                    } else {
                        flowOf(null)
                    }
                }
            }
        }

    /**
     * This emits values when the system bridge needs restarting after it being killed.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val restartFlow: Flow<AutoStartType?> =
        connectionManager.connectionState.flatMapLatest { connectionState ->
            // Do not autostart if it is connected or it was killed from the user
            if (connectionState !is SystemBridgeConnectionState.Disconnected ||
                connectionState.isExpected
            ) {
                flowOf(null)
            } else {
                // Do not autostart if the system bridge was killed shortly after.
                // This prevents infinite loops happening.
                if (lastAutoStartTime != null &&
                    connectionState.time - lastAutoStartTime!! < 30000
                ) {
                    Timber.w(
                        "Not auto starting the system bridge because it was last auto started less than 30 secs ago",
                    )
                    showSystemBridgeKilledNotification(
                        getString(R.string.system_bridge_died_notification_not_restarting_text),
                    )
                    flowOf(null)
                } else {
                    autoStartTypeFlow
                }
            }
        }

    private var lastAutoStartTime: Long? = null

    /**
     * This must only be called once in the application lifecycle
     */
    @OptIn(FlowPreview::class)
    fun init() {
        coroutineScope.launch {
            // The Key Mapper process may not necessarily be started on boot due to the
            // on boot receiver so assume if it is started within a minute of boot that
            // it should be auto started.
            val isBoot = SystemClock.uptimeMillis() < 60000

            if (isBoot) {
                handleAutoStartOnBoot()
            } else if (BuildConfig.DEBUG) {
                Timber.i("Auto starting system bridge because debug build")
                autoStartTypeFlow.first()?.let { autoStart(it) }
            } else {
                handleAutoStartFromPreVersion4()
            }

            // Only start collecting the restart flow after potentially auto starting it for the first time.
            restartFlow
                .distinctUntilChanged() // Must come before the filterNotNull
                .filterNotNull()
                .collectLatest { type ->
                    autoStart(type)
                }
        }
    }

    private suspend fun handleAutoStartOnBoot() {
        // Do not autostart if the device was force rebooted. This may be a sign that PRO mode
        // was broken and the user was trying to reset it.
        val isCleanShutdown = preferences.get(Keys.isCleanShutdown).map { it ?: false }.first()

        Timber.i(
            "SystemBridgeAutoStarter init: isBoot=true, isCleanShutdown=$isCleanShutdown",
        )

        // Reset the value after reading it.
        preferences.set(Keys.isCleanShutdown, false)

        val isBootAutoStartEnabled = preferences.get(Keys.isProModeAutoStartBootEnabled)
            .map { it ?: PreferenceDefaults.PRO_MODE_AUTOSTART_BOOT }
            .first()

        // Wait 5 seconds for the system bridge to potentially connect itself to Key Mapper
        // before starting it.
        delay(5000)

        val connectionState = connectionManager.connectionState.value

        if (isCleanShutdown &&
            isBootAutoStartEnabled &&
            connectionState !is SystemBridgeConnectionState.Connected
        ) {
            val autoStartType = autoStartTypeFlow.first()

            if (autoStartType != null) {
                autoStart(autoStartType)
            }
        }
    }

    private suspend fun handleAutoStartFromPreVersion4() {
        val isFirstTime = preferences.get(Keys.handledRootToProModeUpgrade).first() == null

        if (isFirstTime && suAdapter.isRootGranted.value) {
            Timber.i(
                "Auto starting system bridge because upgraded from pre version 4.0 and was rooted",
            )

            autoStart(AutoStartType.ROOT)
            preferences.set(Keys.handledRootToProModeUpgrade, true)
        }
    }

    private suspend fun autoStart(type: AutoStartType) {
        if (isSystemBridgeEmergencyKilled()) {
            Timber.w(
                "Not auto starting the system bridge because it was emergency killed by the user",
            )
            return
        }

        lastAutoStartTime = SystemClock.elapsedRealtime()

        when (type) {
            AutoStartType.ADB -> {
                Timber.i("Auto starting system bridge with ADB")
                showAutoStartNotification(
                    getString(
                        R.string.pro_mode_setup_notification_auto_start_system_bridge_adb_text,
                    ),
                )

                setupController.autoStartWithAdb()
            }

            AutoStartType.SHIZUKU -> {
                Timber.i("Auto starting system bridge with Shizuku")
                showAutoStartNotification(
                    getString(
                        R.string.pro_mode_setup_notification_auto_start_system_bridge_shizuku_text,
                    ),
                )
                connectionManager.startWithShizuku()
            }

            AutoStartType.ROOT -> {
                Timber.i("Auto starting system bridge with root")
                showAutoStartNotification(
                    getString(
                        R.string.pro_mode_setup_notification_auto_start_system_bridge_root_text,
                    ),
                )
                connectionManager.startWithRoot()
            }
        }

        // Wait 30 seconds for it to start, and if not then show failed notification.
        try {
            withTimeout(30000L) {
                connectionManager.connectionState.first {
                    it is SystemBridgeConnectionState.Connected
                }
            }
        } catch (_: TimeoutCancellationException) {
            showAutoStartFailedNotification()
        }
    }

    private suspend fun isSystemBridgeEmergencyKilled(): Boolean {
        return preferences.get(Keys.isSystemBridgeEmergencyKilled).first() == true
    }

    private fun showSystemBridgeKilledNotification(text: String) {
        val model = NotificationModel(
            id = ID_SYSTEM_BRIDGE_STATUS,
            channel = CHANNEL_SETUP_ASSISTANT,
            title = getString(R.string.system_bridge_died_notification_title),
            text = text,
            icon = R.drawable.pro_mode,
            showOnLockscreen = true,
            onGoing = false,
            priority = NotificationCompat.PRIORITY_MAX,
            autoCancel = true,
            onClickAction = KMNotificationAction.Activity.MainActivity(
                BaseMainActivity.ACTION_START_SYSTEM_BRIDGE,
            ),
            bigTextStyle = true,
        )
        notificationAdapter.showNotification(model)
    }

    private fun showAutoStartNotification(text: String) {
        val model = NotificationModel(
            id = ID_SYSTEM_BRIDGE_STATUS,
            title = getString(R.string.pro_mode_setup_notification_auto_start_system_bridge_title),
            text = text,
            channel = CHANNEL_SETUP_ASSISTANT,
            icon = R.drawable.pro_mode,
            priority = NotificationCompat.PRIORITY_MAX,
            onGoing = true,
            showIndeterminateProgress = true,
            showOnLockscreen = false,
        )

        notificationAdapter.showNotification(model)
    }

    private fun showAutoStartFailedNotification() {
        val model = NotificationModel(
            id = ID_SYSTEM_BRIDGE_STATUS,
            title = getString(
                R.string.pro_mode_setup_notification_start_system_bridge_failed_title,
            ),
            text = getString(R.string.pro_mode_setup_notification_start_system_bridge_failed_text),
            channel = CHANNEL_SETUP_ASSISTANT,
            icon = R.drawable.pro_mode,
            onGoing = false,
            showOnLockscreen = false,
            autoCancel = true,
            priority = NotificationCompat.PRIORITY_MAX,
            onClickAction = KMNotificationAction.Activity.MainActivity(
                BaseMainActivity.ACTION_START_SYSTEM_BRIDGE,
            ),
        )

        notificationAdapter.showNotification(model)
    }
}
