package io.github.sds100.keymapper.base.promode

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import io.github.sds100.keymapper.base.BaseMainActivity
import io.github.sds100.keymapper.base.BuildConfig
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.system.notifications.NotificationController.Companion.CHANNEL_SETUP_ASSISTANT
import io.github.sds100.keymapper.base.system.notifications.NotificationController.Companion.ID_SYSTEM_BRIDGE_STATUS
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.notifications.KMNotificationAction
import io.github.sds100.keymapper.common.utils.Clock
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.sysbridge.manager.isConnected
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
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * This class handles auto starting the system bridge when Key Mapper is launched and when
 * the System Bridge is killed not due to the user.
 */
@RequiresApi(Constants.SYSTEM_BRIDGE_MIN_API)
@Singleton
class SystemBridgeAutoStarter @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val buildConfig: BuildConfigProvider,
    private val clock: Clock,
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
    @SuppressLint("NewApi")
    @OptIn(ExperimentalCoroutinesApi::class)
    private val autoStartTypeFlow: Flow<AutoStartType?> =
        suAdapter.isRootGranted
            .filterNotNull()
            .flatMapLatest { isRooted ->
                if (isRooted) {
                    flowOf(AutoStartType.ROOT)
                } else {
                    val useShizukuFlow =
                        combine(
                            shizukuAdapter.isStarted,
                            permissionAdapter.isGrantedFlow(Permission.SHIZUKU),
                        ) { isStarted, isGranted ->
                            isStarted && isGranted
                        }

                    useShizukuFlow.flatMapLatest { useShizuku ->
                        if (useShizuku) {
                            flowOf(AutoStartType.SHIZUKU)
                        } else if (buildConfig.sdkInt >= Build.VERSION_CODES.R) {
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
                                    if (isAdbAutoStartAllowed) {
                                        AutoStartType.ADB
                                    } else {
                                        null
                                    }
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
    private val autoStartFlow: Flow<AutoStartType?> =
        connectionManager.connectionState.flatMapLatest { connectionState ->
            // Do not autostart if it is connected or it was killed from the user
            if (connectionState !is SystemBridgeConnectionState.Disconnected ||
                connectionState.isStoppedByUser ||
                !getIsUsedBefore() ||
                getIsStoppedByUser() ||
                isSystemBridgeEmergencyKilled() ||
                !isAutoStartEnabled()
            ) {
                flowOf(null)
            } else if (isWithinAutoStartCooldown()) {
                // Do not autostart if the system bridge was killed shortly after.
                // This prevents infinite loops happening.
                Timber.w(
                    "Not auto starting the system bridge because it was last auto started less than 5 mins ago",
                )
                showSystemBridgeKilledNotification(
                    getString(R.string.system_bridge_died_notification_not_restarting_text),
                )
                flowOf(null)
            } else {
                autoStartTypeFlow
            }
        }

    /**
     * This must only be called once in the application lifecycle
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun init() {
        coroutineScope.launch {
            Timber.i(
                "SystemBridgeAutoStarter init: time since boot=${clock.elapsedRealtime() / 1000} seconds",
            )

            // Wait 5 seconds for the system bridge to potentially connect itself to Key Mapper
            // before deciding whether to start it.
            delay(5000)

            if (BuildConfig.DEBUG && connectionManager.isConnected()) {
                // This is useful when developing and need to restart the system bridge
                // after making changes to it.
                Timber.w("Restarting system bridge on debug build.")

                connectionManager.restartSystemBridge()
                delay(5000)
            }

            handleAutoStartFromPreVersion4()

            autoStartFlow
                .distinctUntilChanged() // Must come before the filterNotNull
                .filterNotNull()
                .collectLatest { type ->
                    autoStart(type)
                }
        }
    }

    private suspend fun handleAutoStartFromPreVersion4() {
        @Suppress("DEPRECATION")
        val upgradedFromPreVersion4 = preferences.get(Keys.handledUpgradeToProMode).first() == null

        if (!upgradedFromPreVersion4) {
            return
        }

        val isRooted: Boolean = withTimeoutOrNull(1000) {
            suAdapter.isRootGranted.filterNotNull().first()
        } ?: false

        if (isRooted) {
            Timber.i(
                "Auto starting system bridge because upgraded from pre version 4.0 and was rooted",
            )

            autoStart(AutoStartType.ROOT)
            preferences.set(Keys.handledUpgradeToProMode, true)
            preferences.set(Keys.keyEventActionsUseSystemBridge, true)
            return
        }

        // Try Shizuku after the root check because root is more reliable.
        val isShizukuStarted: Boolean = shizukuAdapter.isStarted.value

        if (isShizukuStarted) {
            Timber.i(
                "Auto starting system bridge because upgraded from pre version 4.0 and Shizuku was started",
            )

            autoStart(AutoStartType.SHIZUKU)
            preferences.set(Keys.handledUpgradeToProMode, true)
            preferences.set(Keys.keyEventActionsUseSystemBridge, true)
            return
        }
    }

    private suspend fun autoStart(type: AutoStartType) {
        if (connectionManager.isConnected()) {
            Timber.i("Not auto starting with $type because already connected.")
            return
        }

        preferences.set(Keys.systemBridgeLastAutoStartTime, clock.elapsedRealtime())

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

    private suspend fun getIsUsedBefore(): Boolean {
        return preferences.get(Keys.isSystemBridgeUsed).first() ?: false
    }

    private suspend fun getIsStoppedByUser(): Boolean {
        return preferences.get(Keys.isSystemBridgeStoppedByUser).first() ?: false
    }

    private suspend fun isSystemBridgeEmergencyKilled(): Boolean {
        return preferences.get(Keys.isSystemBridgeEmergencyKilled).first() == true
    }

    /**
     * Whether the system bridge died less than 5 minutes after the previous time it was
     * auto started.
     */
    private suspend fun isWithinAutoStartCooldown(): Boolean {
        val lastAutoStartTime = preferences.get(Keys.systemBridgeLastAutoStartTime).first()
        return lastAutoStartTime != null &&
            clock.elapsedRealtime() - lastAutoStartTime < (5 * 60_000)
    }

    private suspend fun isAutoStartEnabled(): Boolean {
        return preferences.get(Keys.isSystemBridgeKeepAliveEnabled)
            .map { it ?: PreferenceDefaults.PRO_MODE_KEEP_ALIVE }
            .first()
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
