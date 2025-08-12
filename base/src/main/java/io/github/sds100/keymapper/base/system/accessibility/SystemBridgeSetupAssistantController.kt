package io.github.sds100.keymapper.base.system.accessibility

import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.system.notifications.ManageNotificationsUseCase
import io.github.sds100.keymapper.base.system.notifications.NotificationController
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupController
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupStep
import io.github.sds100.keymapper.system.notifications.NotificationChannelModel
import io.github.sds100.keymapper.system.notifications.NotificationModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber

@Suppress("KotlinConstantConditions")
@RequiresApi(Build.VERSION_CODES.Q)
class SystemBridgeSetupAssistantController @AssistedInject constructor(
    @Assisted
    private val coroutineScope: CoroutineScope,

    @Assisted
    private val accessibilityService: BaseAccessibilityService,

    private val manageNotifications: ManageNotificationsUseCase,
    private val setupController: SystemBridgeSetupController,
    private val preferenceRepository: PreferenceRepository,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    resourceProvider: ResourceProvider
) : ResourceProvider by resourceProvider {
    @AssistedFactory
    interface Factory {
        fun create(
            coroutineScope: CoroutineScope,
            accessibilityService: BaseAccessibilityService
        ): SystemBridgeSetupAssistantController
    }

    companion object {
        /**
         * The max time to spend searching for an accessibility node.
         */
        const val NODE_SEARCH_TIMEOUT = 30000L

        private val PAIRING_CODE_REGEX = Regex("^\\d{6}$")
        private val PORT_REGEX = Regex(".*:([0-9]{1,5})")
    }

    private enum class InteractionStep {
        WIRELESS_DEBUGGING_SWITCH,
        PAIR_DEVICE,
    }

    private val isInteractive: StateFlow<Boolean> =
        preferenceRepository.get(Keys.isProModeInteractiveSetupAssistantEnabled)
            .map { it ?: PreferenceDefaults.PRO_MODE_INTERACTIVE_SETUP_ASSISTANT }
            .stateIn(
                coroutineScope,
                SharingStarted.Lazily,
                PreferenceDefaults.PRO_MODE_INTERACTIVE_SETUP_ASSISTANT
            )

    private var interactionStep: InteractionStep? = null

    /**
     * This job will wait for the interaction timeout and then
     * ask the user to do the steps manually if it failed to do them automatically.
     */
    private var interactionTimeoutJob: Job? = null

    fun onServiceConnected() {
        createNotificationChannel()

        coroutineScope.launch {
            setupController.startSetupAssistantRequest.collect(::startSetupStep)
        }
    }

    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannelModel(
            id = NotificationController.CHANNEL_SETUP_ASSISTANT,
            name = getString(R.string.pro_mode_setup_assistant_notification_channel),
            importance = NotificationManagerCompat.IMPORTANCE_MAX
        )
        manageNotifications.createChannel(notificationChannel)
    }

    fun teardown() {
        // TODO stop showing any notifications
        interactionStep = null
        interactionTimeoutJob?.cancel()
        interactionTimeoutJob = null
    }

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Do not do anything if there is no node to find.
        if (interactionStep == null) {
            return
        }

        // Do not do anything if the interactive setup assistant is disabled
        if (!isInteractive.value) {
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            val step = interactionStep ?: return
            val rootNode = accessibilityService.rootInActiveWindow ?: return

            if (rootNode.packageName != "com.android.settings") {
                return
            }

            doInteractiveStep(step, rootNode)
        }
    }

    private fun doInteractiveStep(step: InteractionStep, rootNode: AccessibilityNodeInfo) {
        when (step) {
            InteractionStep.WIRELESS_DEBUGGING_SWITCH -> TODO()
            InteractionStep.PAIR_DEVICE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    doPairingInteractiveStep(rootNode)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun doPairingInteractiveStep(rootNode: AccessibilityNodeInfo) {
        val pairingCodeText = findPairingCodeText(rootNode)
        val portText = findPortText(rootNode)

        if (pairingCodeText != null && portText != null) {
            val pairingCode = pairingCodeText.toIntOrNull()
            val port = portText.split(":").last().toIntOrNull()

            if (pairingCode != null && port != null) {
                coroutineScope.launch {
                    setupController.pairWirelessAdb(port, pairingCode).onSuccess {
                        setupController.startWithAdb()

                        val isStarted = try {
                            withTimeout(3000L) {
                                systemBridgeConnectionManager.isConnected.first { it }
                            }
                        } catch (e: TimeoutCancellationException) {
                            false
                        }

                        if (isStarted) {
                            val notification = NotificationModel(
                                id = NotificationController.ID_SETUP_ASSISTANT,
                                channel = NotificationController.CHANNEL_SETUP_ASSISTANT,
                                title = getString(R.string.pro_mode_setup_notification_started_success_title),
                                text = getString(R.string.pro_mode_setup_notification_started_success_text),
                                icon = R.drawable.pro_mode,
                                onGoing = false,
                                showOnLockscreen = false,
                                autoCancel = true,
                                bigTextStyle = true
                            )
                            manageNotifications.show(notification)

                            delay(5000)

                            manageNotifications.dismiss(notification.id)
                        } else {
                            // TODO Show notification
                            Timber.w("Failed to start system bridge after pairing.")
                        }
                    }
                }
            }
        }
    }

    private fun findPairingCodeText(rootNode: AccessibilityNodeInfo): String? {
        return rootNode.findNodeRecursively {
            it.text != null && PAIRING_CODE_REGEX.matches(it.text)
        }?.text?.toString()
    }

    private fun findPortText(rootNode: AccessibilityNodeInfo): String? {
        return rootNode.findNodeRecursively {
            it.text != null && PORT_REGEX.matches(it.text)
        }?.text?.toString()
    }

    private fun startSetupStep(step: SystemBridgeSetupStep) {
        Timber.d("Starting setup assistant step: $step")

        when (step) {
            SystemBridgeSetupStep.DEVELOPER_OPTIONS -> {

            }

            SystemBridgeSetupStep.WIRELESS_DEBUGGING -> {}

            SystemBridgeSetupStep.ADB_PAIRING -> interactionStep = InteractionStep.PAIR_DEVICE

            SystemBridgeSetupStep.START_SERVICE -> {}
            else -> {} // Do nothing
        }


        // TODO if finding pairing node does not work, show a notification asking for the pairing code.
        // TODO do this in the timeout job too
    }
}