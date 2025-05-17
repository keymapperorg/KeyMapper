package io.github.sds100.keymapper.system.notifications

import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.sds100.keymapper.base.BaseMainActivity
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.common.result.onFailure
import io.github.sds100.keymapper.common.result.onSuccess
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.system.inputmethod.ShowHideInputMethodUseCase
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.system.inputmethod.ToggleCompatibleImeUseCase
import io.github.sds100.keymapper.base.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.base.util.DispatcherProvider
import io.github.sds100.keymapper.base.util.getFullMessage
import io.github.sds100.keymapper.base.util.ui.ResourceProvider
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
import javax.inject.Inject
import javax.inject.Singleton
import io.github.sds100.keymapper.common.BuildConfigProvider

@Singleton
class NotificationController @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val manageNotifications: ManageNotificationsUseCase,
    private val pauseMappings: PauseKeyMapsUseCase,
    private val showImePicker: ShowInputMethodPickerUseCase,
    private val controlAccessibilityService: ControlAccessibilityServiceUseCase,
    private val toggleCompatibleIme: ToggleCompatibleImeUseCase,
    private val hideInputMethod: ShowHideInputMethodUseCase,
    private val onboardingUseCase: OnboardingUseCase,
    private val resourceProvider: ResourceProvider,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    private val buildConfigProvider: BuildConfigProvider,
) : ResourceProvider by resourceProvider {

    companion object {
        private const val ID_IME_PICKER = 123
        private const val ID_KEYBOARD_HIDDEN = 747
        private const val ID_TOGGLE_MAPPINGS = 231
        private const val ID_TOGGLE_KEYBOARD = 143

        //        private const val ID_FEATURE_ASSISTANT_TRIGGER = 900
        private const val ID_FEATURE_FLOATING_BUTTONS = 901

        const val CHANNEL_TOGGLE_KEYMAPS = "channel_toggle_remaps"
        const val CHANNEL_IME_PICKER = "channel_ime_picker"
        const val CHANNEL_KEYBOARD_HIDDEN = "channel_warning_keyboard_hidden"
        const val CHANNEL_TOGGLE_KEYBOARD = "channel_toggle_keymapper_keyboard"
        const val CHANNEL_NEW_FEATURES = "channel_new_features"

        @Deprecated("Removed in 2.0. This channel shouldn't exist")
        private const val CHANNEL_ID_WARNINGS = "channel_warnings"

        @Deprecated("Removed in 2.0. This channel shouldn't exist")
        private const val CHANNEL_ID_PERSISTENT = "channel_persistent"

        private const val ACTION_RESUME_MAPPINGS =
            "${buildConfigProvider.packageName}.ACTION_RESUME_MAPPINGS"

        private const val ACTION_PAUSE_MAPPINGS = "${buildConfigProvider.packageName}.ACTION_PAUSE_MAPPINGS"

        private const val ACTION_START_SERVICE =
            "${buildConfigProvider.packageName}.ACTION_START_ACCESSIBILITY_SERVICE"

        private const val ACTION_RESTART_SERVICE =
            "${buildConfigProvider.packageName}.ACTION_RESTART_ACCESSIBILITY_SERVICE"

        private const val ACTION_STOP_SERVICE =
            "${buildConfigProvider.packageName}.ACTION_STOP_ACCESSIBILITY_SERVICE"

        private const val ACTION_DISMISS_TOGGLE_MAPPINGS =
            "${buildConfigProvider.packageName}.ACTION_DISMISS_TOGGLE_MAPPINGS"

        private const val ACTION_SHOW_IME_PICKER =
            "${buildConfigProvider.packageName}.ACTION_SHOW_IME_PICKER"
        private const val ACTION_SHOW_KEYBOARD = "${buildConfigProvider.packageName}.ACTION_SHOW_KEYBOARD"

        private const val ACTION_TOGGLE_KEYBOARD =
            "${buildConfigProvider.packageName}.ACTION_TOGGLE_KEYBOARD"
    }

    /**
     * Open the app and use the String as the Intent action.
     */
    private val _openApp: MutableSharedFlow<String> = MutableSharedFlow()
    val openApp: SharedFlow<String> = _openApp.asSharedFlow()

    private val _showToast = MutableSharedFlow<String>()
    val showToast = _showToast.asSharedFlow()

    init {
        manageNotifications.deleteChannel(CHANNEL_ID_WARNINGS)
        manageNotifications.deleteChannel(CHANNEL_ID_PERSISTENT)

        manageNotifications.createChannel(
            NotificationChannelModel(
                id = CHANNEL_NEW_FEATURES,
                name = getString(R.string.notification_channel_new_features),
                NotificationManagerCompat.IMPORTANCE_LOW,
            ),
        )

        combine(
            manageNotifications.showToggleMappingsNotification,
            controlAccessibilityService.serviceState,
            pauseMappings.isPaused,
        ) { show, serviceState, areMappingsPaused ->
            invalidateToggleMappingsNotification(show, serviceState, areMappingsPaused)
        }.flowOn(dispatchers.default()).launchIn(coroutineScope)

        manageNotifications.showImePickerNotification.onEach { show ->
            if (show) {
                manageNotifications.createChannel(
                    NotificationChannelModel(
                        id = CHANNEL_IME_PICKER,
                        name = getString(R.string.notification_channel_ime_picker),
                        NotificationManagerCompat.IMPORTANCE_MIN,
                    ),
                )

                manageNotifications.show(imePickerNotification())
            } else {
                // don't delete the channel because then the user's notification config is lost
                manageNotifications.dismiss(ID_IME_PICKER)
            }
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

        coroutineScope.launch(dispatchers.default()) {
            // suspend until the notification should be shown.
            onboardingUseCase.showFloatingButtonFeatureNotification.first { it }

            manageNotifications.show(floatingButtonFeatureNotification())

            // Only save that the notification is shown if the app has
            // permissions to show notifications so that it is shown
            // the next time permission is granted.
            if (manageNotifications.isPermissionGranted()) {
                onboardingUseCase.showedFloatingButtonFeatureNotification()
            }
        }

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
        }.flowOn(dispatchers.default()).launchIn(coroutineScope)

        manageNotifications.onActionClick.onEach { actionId ->
            when (actionId) {
                ACTION_RESUME_MAPPINGS -> pauseMappings.resume()
                ACTION_PAUSE_MAPPINGS -> pauseMappings.pause()
                ACTION_START_SERVICE -> attemptStartAccessibilityService()
                ACTION_RESTART_SERVICE -> attemptRestartAccessibilityService()
                ACTION_STOP_SERVICE -> controlAccessibilityService.stopService()

                ACTION_DISMISS_TOGGLE_MAPPINGS -> manageNotifications.dismiss(ID_TOGGLE_MAPPINGS)
                ACTION_SHOW_IME_PICKER -> showImePicker.show(fromForeground = false)
                ACTION_SHOW_KEYBOARD -> hideInputMethod.show()
                ACTION_TOGGLE_KEYBOARD -> toggleCompatibleIme.toggle().onSuccess {
                    _showToast.emit(getString(R.string.toast_chose_keyboard, it.label))
                }.onFailure {
                    _showToast.emit(it.getFullMessage(this))
                }
            }
        }.flowOn(dispatchers.default()).launchIn(coroutineScope)
    }

    fun onOpenApp() {
        // show the toggle mappings notification when opening the app in case it has been dismissed

        coroutineScope.launch {
            invalidateToggleMappingsNotification(
                show = manageNotifications.showToggleMappingsNotification.first(),
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
        show: Boolean,
        serviceState: ServiceState,
        areMappingsPaused: Boolean,
    ) {
        manageNotifications.createChannel(
            NotificationChannelModel(
                id = CHANNEL_TOGGLE_KEYMAPS,
                name = getString(R.string.notification_channel_toggle_mappings),
                NotificationManagerCompat.IMPORTANCE_MIN,
            ),
        )

        if (!show) {
            manageNotifications.dismiss(ID_TOGGLE_MAPPINGS)
            return
        }

        when (serviceState) {
            ServiceState.ENABLED -> {
                if (areMappingsPaused) {
                    manageNotifications.show(mappingsPausedNotification())
                } else {
                    manageNotifications.show(mappingsResumedNotification())
                }
            }

            ServiceState.CRASHED ->
                manageNotifications.show(accessibilityServiceCrashedNotification())

            ServiceState.DISABLED ->
                manageNotifications.show(accessibilityServiceDisabledNotification())
        }
    }

    private fun mappingsPausedNotification(): NotificationModel {
        val stopServiceAction = if (controlAccessibilityService.isUserInteractionRequired()) {
            NotificationIntentType.Activity(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        } else {
            NotificationIntentType.Broadcast(ACTION_STOP_SERVICE)
        }

        return NotificationModel(
            id = ID_TOGGLE_MAPPINGS,
            channel = CHANNEL_TOGGLE_KEYMAPS,
            title = getString(R.string.notification_keymaps_paused_title),
            text = getString(R.string.notification_keymaps_paused_text),
            icon = R.drawable.ic_notification_play,
            onClickAction = NotificationIntentType.MainActivity(),
            showOnLockscreen = true,
            onGoing = true,
            priority = NotificationCompat.PRIORITY_MIN,
            actions = listOf(
                NotificationModel.Action(
                    getString(R.string.notification_action_resume),
                    NotificationIntentType.Broadcast(ACTION_RESUME_MAPPINGS),
                ),
                NotificationModel.Action(
                    getString(R.string.notification_action_dismiss),
                    NotificationIntentType.Broadcast(ACTION_DISMISS_TOGGLE_MAPPINGS),
                ),
                NotificationModel.Action(
                    getString(R.string.notification_action_stop_acc_service),
                    stopServiceAction,
                ),
            ),
        )
    }

    private fun mappingsResumedNotification(): NotificationModel {
        // Since Notification trampolines are no longer allowed, the notification
        // must directly launch the accessibility settings instead of relaying the request
        // through a broadcast receiver that eventually calls the ServiceAdapter.
        val stopServiceAction = if (controlAccessibilityService.isUserInteractionRequired()) {
            NotificationIntentType.Activity(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        } else {
            NotificationIntentType.Broadcast(ACTION_STOP_SERVICE)
        }

        return NotificationModel(
            id = ID_TOGGLE_MAPPINGS,
            channel = CHANNEL_TOGGLE_KEYMAPS,
            title = getString(R.string.notification_keymaps_resumed_title),
            text = getString(R.string.notification_keymaps_resumed_text),
            icon = R.drawable.ic_notification_pause,
            onClickAction = NotificationIntentType.MainActivity(),
            showOnLockscreen = true,
            onGoing = true,
            priority = NotificationCompat.PRIORITY_MIN,
            actions = listOf(
                NotificationModel.Action(
                    getString(R.string.notification_action_pause),
                    NotificationIntentType.Broadcast(ACTION_PAUSE_MAPPINGS),
                ),
                NotificationModel.Action(
                    getString(R.string.notification_action_dismiss),
                    NotificationIntentType.Broadcast(ACTION_DISMISS_TOGGLE_MAPPINGS),
                ),
                NotificationModel.Action(
                    getString(R.string.notification_action_stop_acc_service),
                    stopServiceAction,
                ),
            ),
        )
    }

    private fun accessibilityServiceDisabledNotification(): NotificationModel {
        // Since Notification trampolines are no longer allowed, the notification
        // must directly launch the accessibility settings instead of relaying the request
        // through a broadcast receiver that eventually calls the ServiceAdapter.
        val onClickAction = if (controlAccessibilityService.isUserInteractionRequired()) {
            NotificationIntentType.Activity(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        } else {
            NotificationIntentType.Broadcast(ACTION_START_SERVICE)
        }

        return NotificationModel(
            id = ID_TOGGLE_MAPPINGS,
            channel = CHANNEL_TOGGLE_KEYMAPS,
            title = getString(R.string.notification_accessibility_service_disabled_title),
            text = getString(R.string.notification_accessibility_service_disabled_text),
            icon = R.drawable.ic_notification_pause,
            onClickAction = onClickAction,
            showOnLockscreen = true,
            onGoing = true,
            priority = NotificationCompat.PRIORITY_MIN,
            actions = listOf(
                NotificationModel.Action(
                    getString(R.string.notification_action_dismiss),
                    NotificationIntentType.Broadcast(ACTION_DISMISS_TOGGLE_MAPPINGS),
                ),
            ),
        )
    }

    private fun accessibilityServiceCrashedNotification(): NotificationModel {
        // Since Notification trampolines are no longer allowed, the notification
        // must directly launch the accessibility settings instead of relaying the request
        // through a broadcast receiver that eventually calls the ServiceAdapter.
        val onClickAction = if (controlAccessibilityService.isUserInteractionRequired()) {
            NotificationIntentType.Activity(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        } else {
            NotificationIntentType.Broadcast(ACTION_RESTART_SERVICE)
        }

        return NotificationModel(
            id = ID_TOGGLE_MAPPINGS,
            channel = CHANNEL_TOGGLE_KEYMAPS,
            title = getString(R.string.notification_accessibility_service_crashed_title),
            text = getString(R.string.notification_accessibility_service_crashed_text),
            icon = R.drawable.ic_notification_pause,
            onClickAction = onClickAction,
            showOnLockscreen = true,
            onGoing = true,
            priority = NotificationCompat.PRIORITY_MIN,
            bigTextStyle = true,
            actions = listOf(
                NotificationModel.Action(
                    getString(R.string.notification_action_restart_accessibility_service),
                    onClickAction,
                ),
            ),
        )
    }

    private fun imePickerNotification(): NotificationModel = NotificationModel(
        id = ID_IME_PICKER,
        channel = CHANNEL_IME_PICKER,
        title = getString(R.string.notification_ime_persistent_title),
        text = getString(R.string.notification_ime_persistent_text),
        icon = R.drawable.ic_notification_keyboard,
        onClickAction = NotificationIntentType.Broadcast(ACTION_SHOW_IME_PICKER),
        showOnLockscreen = false,
        onGoing = true,
        priority = NotificationCompat.PRIORITY_MIN,
    )

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
            NotificationModel.Action(
                getString(R.string.notification_toggle_keyboard_action),
                intentType = NotificationIntentType.Broadcast(ACTION_TOGGLE_KEYBOARD),
            ),
        ),
    )

    private fun keyboardHiddenNotification(): NotificationModel = NotificationModel(
        id = ID_KEYBOARD_HIDDEN,
        channel = CHANNEL_KEYBOARD_HIDDEN,
        title = getString(R.string.notification_keyboard_hidden_title),
        text = getString(R.string.notification_keyboard_hidden_text),
        icon = R.drawable.ic_notification_keyboard_hide,
        onClickAction = NotificationIntentType.Broadcast(ACTION_SHOW_KEYBOARD),
        showOnLockscreen = false,
        onGoing = true,
        priority = NotificationCompat.PRIORITY_LOW,
    )

    private fun floatingButtonFeatureNotification(): NotificationModel = NotificationModel(
        id = ID_FEATURE_FLOATING_BUTTONS,
        channel = CHANNEL_NEW_FEATURES,
        title = getString(R.string.notification_floating_buttons_feature_title),
        text = getString(R.string.notification_floating_buttons_feature_text),
        icon = R.drawable.outline_bubble_chart_24,
        onClickAction = NotificationIntentType.MainActivity(BaseMainActivity.ACTION_USE_FLOATING_BUTTONS),
        priority = NotificationCompat.PRIORITY_LOW,
        autoCancel = true,
        onGoing = false,
        showOnLockscreen = false,
        bigTextStyle = true,
    )
}
