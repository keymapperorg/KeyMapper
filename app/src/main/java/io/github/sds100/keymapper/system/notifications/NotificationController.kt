package io.github.sds100.keymapper.system.notifications

import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.MainActivity
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.PauseMappingsUseCase
import io.github.sds100.keymapper.mappings.fingerprintmaps.AreFingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.system.inputmethod.ShowHideInputMethodUseCase
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.system.inputmethod.ToggleCompatibleImeUseCase
import io.github.sds100.keymapper.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.util.DispatcherProvider
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.onSuccess
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 24/03/2019.
 */

class NotificationController(
    private val coroutineScope: CoroutineScope,
    private val manageNotifications: ManageNotificationsUseCase,
    private val pauseMappings: PauseMappingsUseCase,
    private val showImePicker: ShowInputMethodPickerUseCase,
    private val controlAccessibilityService: ControlAccessibilityServiceUseCase,
    private val toggleCompatibleIme: ToggleCompatibleImeUseCase,
    private val hideInputMethod: ShowHideInputMethodUseCase,
    private val areFingerprintGesturesSupported: AreFingerprintGesturesSupportedUseCase,
    private val onboardingUseCase: OnboardingUseCase,
    private val resourceProvider: ResourceProvider,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
) : ResourceProvider by resourceProvider {

    companion object {
        private const val ID_IME_PICKER = 123
        private const val ID_KEYBOARD_HIDDEN = 747
        private const val ID_TOGGLE_MAPPINGS = 231
        private const val ID_TOGGLE_KEYBOARD = 143
        private const val ID_FEATURE_REMAP_FINGERPRINT_GESTURES = 1
        const val ID_SETUP_CHOSEN_DEVICES_AGAIN = 2

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
            "${Constants.PACKAGE_NAME}.ACTION_RESUME_MAPPINGS"

        private const val ACTION_PAUSE_MAPPINGS = "${Constants.PACKAGE_NAME}.ACTION_PAUSE_MAPPINGS"

        private const val ACTION_START_SERVICE =
            "${Constants.PACKAGE_NAME}.ACTION_START_ACCESSIBILITY_SERVICE"

        private const val ACTION_RESTART_SERVICE =
            "${Constants.PACKAGE_NAME}.ACTION_RESTART_ACCESSIBILITY_SERVICE"

        private const val ACTION_STOP_SERVICE =
            "${Constants.PACKAGE_NAME}.ACTION_STOP_ACCESSIBILITY_SERVICE"

        private const val ACTION_DISMISS_TOGGLE_MAPPINGS =
            "${Constants.PACKAGE_NAME}.ACTION_DISMISS_TOGGLE_MAPPINGS"

        private const val ACTION_OPEN_KEY_MAPPER =
            "${Constants.PACKAGE_NAME}.ACTION_OPEN_KEY_MAPPER"

        private const val ACTION_SHOW_IME_PICKER =
            "${Constants.PACKAGE_NAME}.ACTION_SHOW_IME_PICKER"
        private const val ACTION_SHOW_KEYBOARD = "${Constants.PACKAGE_NAME}.ACTION_SHOW_KEYBOARD"

        private const val ACTION_TOGGLE_KEYBOARD =
            "${Constants.PACKAGE_NAME}.ACTION_TOGGLE_KEYBOARD"

        @VisibleForTesting
        const val ACTION_ON_SETUP_CHOSEN_DEVICES_AGAIN =
            "${Constants.PACKAGE_NAME}.ACTION_ON_SETUP_CHOSEN_DEVICES_AGAIN"

        private const val ACTION_FINGERPRINT_GESTURE_FEATURE =
            "${Constants.PACKAGE_NAME}.ACTION_FINGERPRINT_GESTURE_FEATURE"
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
            combine(
                onboardingUseCase.showFingerprintFeatureNotificationIfAvailable,
                areFingerprintGesturesSupported.isSupported.map { it ?: false },
            ) { showIfAvailable, isSupported ->
                showIfAvailable && isSupported
            }.first { it } // suspend until the notification should be shown

            manageNotifications.createChannel(
                NotificationChannelModel(
                    id = CHANNEL_NEW_FEATURES,
                    name = getString(R.string.notification_channel_new_features),
                    NotificationManagerCompat.IMPORTANCE_LOW,
                ),
            )

            manageNotifications.show(fingerprintFeatureNotification())
            onboardingUseCase.showedFingerprintFeatureNotificationIfAvailable()
        }

        onboardingUseCase.showSetupChosenDevicesAgainNotification.onEach { show ->
            if (show) {
                manageNotifications.show(setupChosenDevicesSettingsAgainNotification())
            } else {
                manageNotifications.dismiss(ID_SETUP_CHOSEN_DEVICES_AGAIN)
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
        }.flowOn(dispatchers.default()).launchIn(coroutineScope)

        manageNotifications.onActionClick.onEach { actionId ->
            when (actionId) {
                ACTION_RESUME_MAPPINGS -> pauseMappings.resume()
                ACTION_PAUSE_MAPPINGS -> pauseMappings.pause()
                ACTION_START_SERVICE -> attemptStartAccessibilityService()
                ACTION_RESTART_SERVICE -> attemptRestartAccessibilityService()
                ACTION_STOP_SERVICE -> controlAccessibilityService.stopService()

                ACTION_DISMISS_TOGGLE_MAPPINGS -> manageNotifications.dismiss(ID_TOGGLE_MAPPINGS)
                ACTION_OPEN_KEY_MAPPER -> _openApp.emit("")
                ACTION_SHOW_IME_PICKER -> showImePicker.show(fromForeground = false)
                ACTION_SHOW_KEYBOARD -> hideInputMethod.show()
                ACTION_TOGGLE_KEYBOARD -> toggleCompatibleIme.toggle().onSuccess {
                    _showToast.emit(getString(R.string.toast_chose_keyboard, it.label))
                }.onFailure {
                    _showToast.emit(it.getFullMessage(this))
                }

                ACTION_FINGERPRINT_GESTURE_FEATURE -> {
                    onboardingUseCase.approvedFingerprintFeaturePrompt = true
                    _openApp.emit("")
                }

                ACTION_ON_SETUP_CHOSEN_DEVICES_AGAIN -> {
                    onboardingUseCase.approvedSetupChosenDevicesAgainNotification()
                    _openApp.emit("")
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
                _openApp.emit(MainActivity.ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG)
            }
        }
    }

    private fun attemptRestartAccessibilityService() {
        if (!controlAccessibilityService.restartService()) {
            coroutineScope.launch {
                _openApp.emit(MainActivity.ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG)
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

    private fun mappingsPausedNotification(): NotificationModel = NotificationModel(
        id = ID_TOGGLE_MAPPINGS,
        channel = CHANNEL_TOGGLE_KEYMAPS,
        title = getString(R.string.notification_keymaps_paused_title),
        text = getString(R.string.notification_keymaps_paused_text),
        icon = R.drawable.ic_notification_play,
        onClickActionId = ACTION_OPEN_KEY_MAPPER,
        showOnLockscreen = true,
        onGoing = true,
        priority = NotificationCompat.PRIORITY_MIN,
        actions = listOf(
            NotificationModel.Action(
                ACTION_RESUME_MAPPINGS,
                getString(R.string.notification_action_resume),
            ),
            NotificationModel.Action(
                ACTION_DISMISS_TOGGLE_MAPPINGS,
                getString(R.string.notification_action_dismiss),
            ),
            NotificationModel.Action(
                ACTION_STOP_SERVICE,
                getString(R.string.notification_action_stop_acc_service),
            ),
        ),
    )

    private fun mappingsResumedNotification(): NotificationModel = NotificationModel(
        id = ID_TOGGLE_MAPPINGS,
        channel = CHANNEL_TOGGLE_KEYMAPS,
        title = getString(R.string.notification_keymaps_resumed_title),
        text = getString(R.string.notification_keymaps_resumed_text),
        icon = R.drawable.ic_notification_pause,
        onClickActionId = ACTION_OPEN_KEY_MAPPER,
        showOnLockscreen = true,
        onGoing = true,
        priority = NotificationCompat.PRIORITY_MIN,
        actions = listOf(
            NotificationModel.Action(
                ACTION_PAUSE_MAPPINGS,
                getString(R.string.notification_action_pause),
            ),
            NotificationModel.Action(
                ACTION_DISMISS_TOGGLE_MAPPINGS,
                getString(R.string.notification_action_dismiss),
            ),
            NotificationModel.Action(
                ACTION_STOP_SERVICE,
                getString(R.string.notification_action_stop_acc_service),
            ),
        ),
    )

    private fun accessibilityServiceDisabledNotification(): NotificationModel = NotificationModel(
        id = ID_TOGGLE_MAPPINGS,
        channel = CHANNEL_TOGGLE_KEYMAPS,
        title = getString(R.string.notification_accessibility_service_disabled_title),
        text = getString(R.string.notification_accessibility_service_disabled_text),
        icon = R.drawable.ic_notification_pause,
        onClickActionId = ACTION_START_SERVICE,
        showOnLockscreen = true,
        onGoing = true,
        priority = NotificationCompat.PRIORITY_MIN,
        actions = listOf(
            NotificationModel.Action(
                ACTION_DISMISS_TOGGLE_MAPPINGS,
                getString(R.string.notification_action_dismiss),
            ),
        ),
    )

    private fun accessibilityServiceCrashedNotification(): NotificationModel = NotificationModel(
        id = ID_TOGGLE_MAPPINGS,
        channel = CHANNEL_TOGGLE_KEYMAPS,
        title = getString(R.string.notification_accessibility_service_crashed_title),
        text = getString(R.string.notification_accessibility_service_crashed_text),
        icon = R.drawable.ic_notification_pause,
        onClickActionId = ACTION_RESTART_SERVICE,
        showOnLockscreen = true,
        onGoing = true,
        priority = NotificationCompat.PRIORITY_MIN,
        bigTextStyle = true,
        actions = listOf(
            NotificationModel.Action(
                ACTION_RESTART_SERVICE,
                getString(R.string.notification_action_restart_accessibility_service),
            ),
        ),
    )

    private fun imePickerNotification(): NotificationModel = NotificationModel(
        id = ID_IME_PICKER,
        channel = CHANNEL_IME_PICKER,
        title = getString(R.string.notification_ime_persistent_title),
        text = getString(R.string.notification_ime_persistent_text),
        icon = R.drawable.ic_notification_keyboard,
        onClickActionId = ACTION_SHOW_IME_PICKER,
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
        onClickActionId = null,
        showOnLockscreen = true,
        onGoing = true,
        priority = NotificationCompat.PRIORITY_MIN,
        actions = listOf(
            NotificationModel.Action(
                ACTION_TOGGLE_KEYBOARD,
                getString(R.string.notification_toggle_keyboard_action),
            ),
        ),
    )

    private fun fingerprintFeatureNotification(): NotificationModel = NotificationModel(
        id = ID_FEATURE_REMAP_FINGERPRINT_GESTURES,
        channel = CHANNEL_NEW_FEATURES,
        title = getString(R.string.notification_feature_fingerprint_title),
        text = getString(R.string.notification_feature_fingerprint_text),
        icon = R.drawable.ic_notification_fingerprint,
        onClickActionId = ACTION_FINGERPRINT_GESTURE_FEATURE,
        priority = NotificationCompat.PRIORITY_LOW,
        autoCancel = true,
        onGoing = false,
        showOnLockscreen = false,
        bigTextStyle = true,
    )

    private fun setupChosenDevicesSettingsAgainNotification(): NotificationModel =
        NotificationModel(
            id = ID_SETUP_CHOSEN_DEVICES_AGAIN,
            channel = CHANNEL_NEW_FEATURES,
            title = getString(R.string.notification_setup_chosen_devices_again_title),
            text = getString(R.string.notification_setup_chosen_devices_again_text),
            icon = R.drawable.ic_notification_settings,
            onClickActionId = ACTION_ON_SETUP_CHOSEN_DEVICES_AGAIN,
            priority = NotificationCompat.PRIORITY_LOW,
            autoCancel = true,
            onGoing = false,
            showOnLockscreen = false,
            bigTextStyle = true,
        )

    private fun keyboardHiddenNotification(): NotificationModel = NotificationModel(
        id = ID_KEYBOARD_HIDDEN,
        channel = CHANNEL_KEYBOARD_HIDDEN,
        title = getString(R.string.notification_keyboard_hidden_title),
        text = getString(R.string.notification_keyboard_hidden_text),
        icon = R.drawable.ic_notification_keyboard_hide,
        onClickActionId = ACTION_SHOW_KEYBOARD,
        showOnLockscreen = false,
        onGoing = true,
        priority = NotificationCompat.PRIORITY_LOW,
    )
}
