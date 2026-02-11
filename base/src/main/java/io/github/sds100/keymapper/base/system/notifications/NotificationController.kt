package io.github.sds100.keymapper.base.system.notifications

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.sds100.keymapper.base.BaseMainActivity
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.base.system.inputmethod.ShowHideInputMethodUseCase
import io.github.sds100.keymapper.base.system.inputmethod.ToggleCompatibleImeUseCase
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.notifications.KMNotificationAction
import io.github.sds100.keymapper.common.utils.DefaultDispatcherProvider
import io.github.sds100.keymapper.common.utils.DispatcherProvider
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import io.github.sds100.keymapper.system.notifications.NotificationChannelModel
import io.github.sds100.keymapper.system.notifications.NotificationModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Singleton
class NotificationController @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val manageNotifications: ManageNotificationsUseCase,
    private val pauseMappings: PauseKeyMapsUseCase,
    private val controlAccessibilityService: ControlAccessibilityServiceUseCase,
    private val toggleCompatibleIme: ToggleCompatibleImeUseCase,
    private val hideInputMethod: ShowHideInputMethodUseCase,
    private val onboardingUseCase: OnboardingUseCase,
    private val resourceProvider: ResourceProvider,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
) : ResourceProvider by resourceProvider {

    companion object {
        //        private const val ID_IME_PICKER = 123
        private const val ID_KEYBOARD_HIDDEN = 747
        private const val ID_TOGGLE_MAPPINGS = 231
        private const val ID_TOGGLE_KEYBOARD = 143
        const val ID_SETUP_ASSISTANT = 144
        const val ID_SYSTEM_BRIDGE_STATUS = 145

        //        private const val ID_FEATURE_ASSISTANT_TRIGGER = 900
        private const val ID_FEATURE_FLOATING_BUTTONS = 901
        private const val ID_MIGRATE_SCREEN_OFF_KEY_MAPS = 902

        const val CHANNEL_TOGGLE_KEY_MAPS = "channel_toggle_remaps"

        @Deprecated("Removed in 4.0.0")
        const val CHANNEL_IME_PICKER = "channel_ime_picker"

        const val CHANNEL_KEYBOARD_HIDDEN = "channel_warning_keyboard_hidden"
        const val CHANNEL_TOGGLE_KEYBOARD = "channel_toggle_keymapper_keyboard"
        const val CHANNEL_NEW_FEATURES = "channel_new_features"
        const val CHANNEL_SETUP_ASSISTANT = "channel_setup_assistant"
        const val CHANNEL_VERSION_MIGRATION = "channel_version_migration"
        const val CHANNEL_CUSTOM_NOTIFICATIONS = "channel_custom_notifications"

        @Deprecated("Removed in 2.0. This channel shouldn't exist")
        private const val CHANNEL_ID_WARNINGS = "channel_warnings"

        @Deprecated("Removed in 2.0. This channel shouldn't exist")
        private const val CHANNEL_ID_PERSISTENT = "channel_persistent"
    }

    /**
     * Open the app and use the String as the Intent action.
     */
    private val _openApp: MutableSharedFlow<String> = MutableSharedFlow()
    val openApp: SharedFlow<String> = _openApp.asSharedFlow()

    private val _showToast = MutableSharedFlow<String>()
    val showToast = _showToast.asSharedFlow()

    fun init() {
        manageNotifications.deleteChannel(CHANNEL_ID_WARNINGS)
        manageNotifications.deleteChannel(CHANNEL_ID_PERSISTENT)
        manageNotifications.deleteChannel(CHANNEL_IME_PICKER)

        manageNotifications.createChannel(
            NotificationChannelModel(
                id = CHANNEL_NEW_FEATURES,
                name = getString(R.string.notification_channel_new_features),
                NotificationManagerCompat.IMPORTANCE_LOW,
            ),
        )

        manageNotifications.createChannel(
            NotificationChannelModel(
                id = CHANNEL_SETUP_ASSISTANT,
                name = getString(R.string.expert_mode_setup_assistant_notification_channel),
                importance = NotificationManagerCompat.IMPORTANCE_MAX,
            ),
        )

        manageNotifications.createChannel(
            NotificationChannelModel(
                id = CHANNEL_CUSTOM_NOTIFICATIONS,
                name = getString(R.string.notification_channel_custom_notifications),
                NotificationManagerCompat.IMPORTANCE_DEFAULT,
            ),
        )

        combine(
            controlAccessibilityService.serviceState,
            pauseMappings.isPaused,
        ) { serviceState, areMappingsPaused ->
            invalidateToggleMappingsNotification(serviceState, areMappingsPaused)
        }.flowOn(dispatchers.default()).launchIn(coroutineScope)

        toggleCompatibleIme.sufficientPermissions.onEach { canToggleIme ->
            if (canToggleIme) {
                manageNotifications.createChannel(
                    NotificationChannelModel(
                        id = CHANNEL_TOGGLE_KEYBOARD,
                        name = getString(R.string.notification_channel_toggle_keyboard),
                        NotificationManagerCompat.IMPORTANCE_MIN,
                    ),
                )

                manageNotifications.show(toggleImeNotification())
            } else {
                // don't delete the channel because then the user's notification config is lost
                manageNotifications.dismiss(ID_TOGGLE_KEYBOARD)
            }
        }.flowOn(dispatchers.default()).launchIn(coroutineScope)

        hideInputMethod.onHiddenChange.onEach { isHidden ->
            manageNotifications.createChannel(
                NotificationChannelModel(
                    id = CHANNEL_KEYBOARD_HIDDEN,
                    name = getString(R.string.notification_channel_keyboard_hidden),
                    NotificationManagerCompat.IMPORTANCE_DEFAULT,
                ),
            )

            if (isHidden) {
                manageNotifications.show(keyboardHiddenNotification())
            } else {
                manageNotifications.dismiss(ID_KEYBOARD_HIDDEN)
            }
        }.launchIn(coroutineScope)

        manageNotifications.onActionClick.onEach { actionId ->
            when (actionId) {
                KMNotificationAction.IntentAction.RESUME_KEY_MAPS -> pauseMappings.resume()

                KMNotificationAction.IntentAction.PAUSE_KEY_MAPS -> pauseMappings.pause()

                KMNotificationAction.IntentAction.DISMISS_TOGGLE_KEY_MAPS_NOTIFICATION ->
                    manageNotifications.dismiss(ID_TOGGLE_MAPPINGS)

                KMNotificationAction.IntentAction.STOP_ACCESSIBILITY_SERVICE ->
                    controlAccessibilityService.stopService()

                KMNotificationAction.IntentAction.START_ACCESSIBILITY_SERVICE ->
                    attemptStartAccessibilityService()

                KMNotificationAction.IntentAction.RESTART_ACCESSIBILITY_SERVICE ->
                    attemptRestartAccessibilityService()

                KMNotificationAction.IntentAction.TOGGLE_KEY_MAPPER_IME ->
                    toggleCompatibleIme.toggle()
                        .onSuccess {
                            _showToast.emit(getString(R.string.toast_chose_keyboard, it.label))
                        }.onFailure {
                            _showToast.emit(it.getFullMessage(this))
                        }

                KMNotificationAction.IntentAction.SHOW_KEYBOARD -> hideInputMethod.show()

                else -> Unit // Ignore other notification actions
            }
        }.launchIn(coroutineScope)

        coroutineScope.launch {
            systemBridgeConnectionManager.connectionState
                .collect { connectionState ->
                    if (connectionState is SystemBridgeConnectionState.Connected) {
                        showSystemBridgeStartedNotification()
                    }
                }
        }

        coroutineScope.launch {
            if (onboardingUseCase.showMigrateScreenOffKeyMapsNotification.first()) {
                manageNotifications.show(
                    NotificationModel(
                        id = ID_MIGRATE_SCREEN_OFF_KEY_MAPS,
                        channel = CHANNEL_NEW_FEATURES,
                        title = getString(R.string.notification_migrate_screen_off_key_map_title),
                        text = getString(R.string.notification_migrate_screen_off_key_map_text),
                        icon = R.drawable.ic_baseline_warning_24,
                        onClickAction = KMNotificationAction.Activity.MainActivity(),
                        showOnLockscreen = true,
                        onGoing = false,
                    ),
                )
            }
        }
    }

    fun onOpenApp() {
        // show the toggle mappings notification when opening the app in case it has been dismissed

        coroutineScope.launch {
            invalidateToggleMappingsNotification(
                serviceState = controlAccessibilityService.serviceState.first(),
                areMappingsPaused = pauseMappings.isPaused.first(),
            )
        }
    }

    private fun attemptStartAccessibilityService() {
        if (!controlAccessibilityService.startService()) {
            coroutineScope.launch {
                _openApp.emit(BaseMainActivity.ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG)
            }
        }
    }

    private fun attemptRestartAccessibilityService() {
        if (!controlAccessibilityService.restartService()) {
            coroutineScope.launch {
                _openApp.emit(BaseMainActivity.ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG)
            }
        }
    }

    private fun invalidateToggleMappingsNotification(
        serviceState: AccessibilityServiceState,
        areMappingsPaused: Boolean,
    ) {
        manageNotifications.createChannel(
            NotificationChannelModel(
                id = CHANNEL_TOGGLE_KEY_MAPS,
                name = getString(R.string.notification_channel_toggle_mappings),
                NotificationManagerCompat.IMPORTANCE_MIN,
            ),
        )

        when (serviceState) {
            AccessibilityServiceState.ENABLED -> {
                if (areMappingsPaused) {
                    manageNotifications.show(mappingsPausedNotification())
                } else {
                    manageNotifications.show(mappingsResumedNotification())
                }
            }

            AccessibilityServiceState.CRASHED ->
                manageNotifications.show(accessibilityServiceCrashedNotification())

            AccessibilityServiceState.DISABLED ->
                manageNotifications.show(accessibilityServiceDisabledNotification())
        }
    }

    private fun mappingsPausedNotification(): NotificationModel {
        // Since Notification trampolines are no longer allowed, the notification
        // must directly launch the accessibility settings instead of relaying the request
        // through a broadcast receiver that eventually calls the ServiceAdapter.
        val stopServiceAction = if (controlAccessibilityService.isUserInteractionRequired()) {
            KMNotificationAction.Activity.AccessibilitySettings
        } else {
            KMNotificationAction.Broadcast.StopAccessibilityService
        }

        return NotificationModel(
            id = ID_TOGGLE_MAPPINGS,
            channel = CHANNEL_TOGGLE_KEY_MAPS,
            title = getString(R.string.notification_keymaps_paused_title),
            text = getString(R.string.notification_keymaps_paused_text),
            icon = R.drawable.ic_notification_play,
            onClickAction = KMNotificationAction.Activity.MainActivity(),
            showOnLockscreen = true,
            onGoing = true,
            priority = NotificationCompat.PRIORITY_MIN,
            actions = listOf(
                KMNotificationAction.Broadcast.ResumeKeyMaps to
                    getString(R.string.notification_action_resume),
                KMNotificationAction.Broadcast.DismissToggleKeyMapsNotification to
                    getString(R.string.notification_action_dismiss),
                stopServiceAction to getString(R.string.notification_action_stop_acc_service),
            ),
        )
    }

    private fun mappingsResumedNotification(): NotificationModel {
        // Since Notification trampolines are no longer allowed, the notification
        // must directly launch the accessibility settings instead of relaying the request
        // through a broadcast receiver that eventually calls the ServiceAdapter.
        val stopServiceAction = if (controlAccessibilityService.isUserInteractionRequired()) {
            KMNotificationAction.Activity.AccessibilitySettings
        } else {
            KMNotificationAction.Broadcast.StopAccessibilityService
        }

        return NotificationModel(
            id = ID_TOGGLE_MAPPINGS,
            channel = CHANNEL_TOGGLE_KEY_MAPS,
            title = getString(R.string.notification_keymaps_resumed_title),
            text = getString(R.string.notification_keymaps_resumed_text),
            icon = R.drawable.ic_notification_pause,
            onClickAction = KMNotificationAction.Activity.MainActivity(),
            showOnLockscreen = true,
            onGoing = true,
            priority = NotificationCompat.PRIORITY_MIN,
            actions = listOf(
                KMNotificationAction.Broadcast.PauseKeyMaps to
                    getString(R.string.notification_action_pause),
                KMNotificationAction.Broadcast.DismissToggleKeyMapsNotification to
                    getString(R.string.notification_action_dismiss),
                stopServiceAction to getString(R.string.notification_action_stop_acc_service),
            ),
        )
    }

    private fun accessibilityServiceDisabledNotification(): NotificationModel {
        // Since Notification trampolines are no longer allowed, the notification
        // must directly launch the accessibility settings instead of relaying the request
        // through a broadcast receiver that eventually calls the ServiceAdapter.
        val startServiceAction = if (controlAccessibilityService.isUserInteractionRequired()) {
            KMNotificationAction.Activity.AccessibilitySettings
        } else {
            KMNotificationAction.Broadcast.StartAccessibilityService
        }

        return NotificationModel(
            id = ID_TOGGLE_MAPPINGS,
            channel = CHANNEL_TOGGLE_KEY_MAPS,
            title = getString(R.string.notification_accessibility_service_disabled_title),
            text = getString(R.string.notification_accessibility_service_disabled_text),
            icon = R.drawable.ic_notification_pause,
            onClickAction = startServiceAction,
            showOnLockscreen = true,
            onGoing = true,
            priority = NotificationCompat.PRIORITY_MIN,
            actions = listOf(
                KMNotificationAction.Broadcast.DismissToggleKeyMapsNotification to
                    getString(R.string.notification_action_dismiss),

            ),
        )
    }

    private fun accessibilityServiceCrashedNotification(): NotificationModel {
        // Since Notification trampolines are no longer allowed, the notification
        // must directly launch the accessibility settings instead of relaying the request
        // through a broadcast receiver that eventually calls the ServiceAdapter.
        val restartServiceAction = if (controlAccessibilityService.isUserInteractionRequired()) {
            KMNotificationAction.Activity.AccessibilitySettings
        } else {
            KMNotificationAction.Broadcast.RestartAccessibilityService
        }

        return NotificationModel(
            id = ID_TOGGLE_MAPPINGS,
            channel = CHANNEL_TOGGLE_KEY_MAPS,
            title = getString(R.string.notification_accessibility_service_crashed_title),
            text = getString(R.string.notification_accessibility_service_crashed_text),
            icon = R.drawable.ic_notification_pause,
            onClickAction = restartServiceAction,
            showOnLockscreen = true,
            onGoing = true,
            priority = NotificationCompat.PRIORITY_MIN,
            bigTextStyle = true,
            actions = listOf(
                restartServiceAction to
                    getString(R.string.notification_action_restart_accessibility_service),
            ),
        )
    }

    private fun toggleImeNotification(): NotificationModel = NotificationModel(
        id = ID_TOGGLE_KEYBOARD,
        channel = CHANNEL_TOGGLE_KEYBOARD,
        title = getString(R.string.notification_toggle_keyboard_title),
        text = getString(R.string.notification_toggle_keyboard_text),
        icon = R.drawable.ic_notification_keyboard,
        showOnLockscreen = true,
        onGoing = true,
        priority = NotificationCompat.PRIORITY_MIN,
        actions = listOf(
            KMNotificationAction.Broadcast.TogglerKeyMapperIme to
                getString(R.string.notification_toggle_keyboard_action),
        ),
    )

    private fun keyboardHiddenNotification(): NotificationModel = NotificationModel(
        id = ID_KEYBOARD_HIDDEN,
        channel = CHANNEL_KEYBOARD_HIDDEN,
        title = getString(R.string.notification_keyboard_hidden_title),
        text = getString(R.string.notification_keyboard_hidden_text),
        icon = R.drawable.ic_notification_keyboard_hide,
        onClickAction = KMNotificationAction.Broadcast.ShowKeyboard,
        showOnLockscreen = false,
        onGoing = true,
        priority = NotificationCompat.PRIORITY_LOW,
    )

    private fun showSystemBridgeStartedNotification() {
        val model = NotificationModel(
            id = ID_SYSTEM_BRIDGE_STATUS,
            title = getString(R.string.expert_mode_setup_notification_system_bridge_started_title),
            text = getString(R.string.expert_mode_setup_notification_system_bridge_started_text),
            channel = CHANNEL_SETUP_ASSISTANT,
            icon = R.drawable.offline_bolt_24px,
            onGoing = false,
            showOnLockscreen = false,
            autoCancel = true,
            timeout = 5000,
        )

        manageNotifications.show(model)
    }
}
