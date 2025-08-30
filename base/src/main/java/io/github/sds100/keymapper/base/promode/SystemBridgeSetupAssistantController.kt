package io.github.sds100.keymapper.base.promode

import android.app.ActivityManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.sds100.keymapper.base.BaseMainActivity
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.system.accessibility.BaseAccessibilityService
import io.github.sds100.keymapper.base.system.accessibility.findNodeRecursively
import io.github.sds100.keymapper.base.system.notifications.ManageNotificationsUseCase
import io.github.sds100.keymapper.base.system.notifications.NotificationController
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.KeyMapperClassProvider
import io.github.sds100.keymapper.common.notifications.KMNotificationAction
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
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
    private val keyMapperClassProvider: KeyMapperClassProvider,
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
        const val INTERACTION_TIMEOUT = 10000L

        private val PAIRING_CODE_REGEX = Regex("^\\d{6}$")
        private val IPV4_REGEX =
            Regex("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
    }

    private enum class InteractionStep {
        // Do not automatically turn on the wireless debugging switch. When the user turns it on,
        // Key Mapper will automatically pair.
        PAIR_DEVICE,
    }

    private val activityManager: ActivityManager = accessibilityService.getSystemService()!!

    private val isInteractive: StateFlow<Boolean> =
        preferenceRepository.get(Keys.isProModeInteractiveSetupAssistantEnabled)
            .map { it ?: PreferenceDefaults.PRO_MODE_INTERACTIVE_SETUP_ASSISTANT }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                PreferenceDefaults.PRO_MODE_INTERACTIVE_SETUP_ASSISTANT
            )

    private var interactionStep: InteractionStep? = null

    /**
     * This job will wait for the interaction timeout and then
     * ask the user to do the steps manually if it failed to do them automatically.
     */
    private var interactionTimeoutJob: Job? = null

    // Store the pairing code so only one request to pair is sent per pairing code.
    private var foundPairingCode: String? = null

    fun onServiceConnected() {
        createNotificationChannel()

        coroutineScope.launch {
            setupController.setupAssistantStep.collect { step ->
                if (step == null) {
                    stopInteracting()
                    dismissNotification()
                } else {
                    startSetupStep(step)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            coroutineScope.launch {
                manageNotifications.onNotificationTextInput
                    .filter { it.intentAction == KMNotificationAction.IntentAction.PAIRING_CODE_REPLY }
                    .collect { textInput ->
                        Timber.i("Receive pairing code text input: $textInput")

                        val pairingCode: String = textInput.text.trim()
                        onPairingCodeFound(pairingCode)
                    }
            }
        }
    }

    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannelModel(
            id = NotificationController.Companion.CHANNEL_SETUP_ASSISTANT,
            name = getString(R.string.pro_mode_setup_assistant_notification_channel),
            importance = NotificationManagerCompat.IMPORTANCE_MAX
        )
        manageNotifications.createChannel(notificationChannel)
    }

    fun teardown() {
        dismissNotification()
        stopInteracting()
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

        if (pairingCodeText == null) {
            clickPairWithCodeButton(rootNode)
        } else {
            val pairingCode = pairingCodeText.trim()

            coroutineScope.launch {
                onPairingCodeFound(pairingCode)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun onPairingCodeFound(pairingCode: String) {
        // Only pair once per pairing code.
        if (foundPairingCode == pairingCode) {
            return
        }

        foundPairingCode = pairingCode

        Timber.i("Pairing code found ${pairingCode}. Pairing ADB...")
        setupController.pairWirelessAdb(pairingCode).onSuccess {
            onPairingSuccess()
        }.onFailure {
            Timber.e("Failed to pair with wireless ADB: $it")
            stopInteracting()

            showNotification(
                getString(R.string.pro_mode_setup_notification_invalid_pairing_code_title),
                getString(R.string.pro_mode_setup_notification_invalid_pairing_code_text),
                actions = listOf(KMNotificationAction.RemoteInput.PairingCode to getString(R.string.pro_mode_setup_notification_action_input_pairing_code))
            )
        }
    }

    private suspend fun onPairingSuccess() {
        setupController.startWithAdb()

        stopInteracting()

        val isStarted = try {
            withTimeout(10000L) {
                systemBridgeConnectionManager.connectionState
                    .filterIsInstance<SystemBridgeConnectionState.Connected>()
                    .first()
            }

            true
        } catch (_: TimeoutCancellationException) {
            false
        }

        if (isStarted) {
            getKeyMapperAppTask()?.moveToFront()
        } else {
            Timber.e("Failed to start system bridge after pairing.")
            showNotification(
                getString(R.string.pro_mode_setup_notification_start_system_bridge_failed_title),
                getString(R.string.pro_mode_setup_notification_start_system_bridge_failed_text),
                onClickAction = KMNotificationAction.Activity.MainActivity(BaseMainActivity.ACTION_START_SYSTEM_BRIDGE)
            )
        }
    }

    private fun clickPairWithCodeButton(rootNode: AccessibilityNodeInfo) {
        rootNode
            .findNodeRecursively { it.className == "androidx.recyclerview.widget.RecyclerView" }
            ?.takeIf { recyclerView ->
                // There are many settings screens with RecyclerViews so make sure
                // the correct page is showing before clicking. It is not as simple
                // as checking the words on the screen due to different languages.
                val ipAddressPortText: CharSequence? =
                    runCatching {
                        // RecyclerView -> LinearLayout -> RelativeLayout -> TextView
                        recyclerView.getChild(1).getChild(0).getChild(1)
                    }.getOrNull()?.text

                val ipText = ipAddressPortText?.split(":")?.firstOrNull()
                ipText != null && IPV4_REGEX.matches(ipText)
            }
            ?.runCatching { getChild(3) }
            ?.getOrNull()
            ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun showNotification(
        title: String,
        text: String,
        onClickAction: KMNotificationAction? = null,
        actions: List<Pair<KMNotificationAction, String>> = emptyList()
    ) {
        val notification = NotificationModel(
            // Use the same notification id for all so they overwrite each other.
            id = NotificationController.Companion.ID_SETUP_ASSISTANT,
            channel = NotificationController.Companion.CHANNEL_SETUP_ASSISTANT,
            title = title,
            text = text,
            icon = R.drawable.pro_mode,
            onGoing = false,
            showOnLockscreen = false,
            autoCancel = true,
            onClickAction = onClickAction,
            // Must not be silent so it is shown as a heads up notification
            silent = false,
            actions = actions
        )
        manageNotifications.show(notification)
    }

    private fun dismissNotification() {
        manageNotifications.dismiss(NotificationController.Companion.ID_SETUP_ASSISTANT)
    }

    private fun findPairingCodeText(rootNode: AccessibilityNodeInfo): String? {
        return rootNode.findNodeRecursively {
            it.text != null && PAIRING_CODE_REGEX.matches(it.text)
        }?.text?.toString()
    }

    private fun startSetupStep(step: SystemBridgeSetupStep) {
        Timber.i("Starting setup assistant step: $step")
        when (step) {
            SystemBridgeSetupStep.DEVELOPER_OPTIONS -> {
                showNotification(
                    getString(R.string.pro_mode_setup_notification_tap_build_number_title),
                    getString(R.string.pro_mode_setup_notification_tap_build_number_text),
                )
            }

            SystemBridgeSetupStep.ADB_PAIRING -> {
                showNotification(
                    getString(R.string.pro_mode_setup_notification_pairing_title),
                    getString(R.string.pro_mode_setup_notification_pairing_text),
                )

                interactionStep = InteractionStep.PAIR_DEVICE
            }

            else -> return // Do not start interaction timeout job
        }

        startInteractionTimeoutJob()
    }

    private fun startInteractionTimeoutJob() {
        interactionTimeoutJob?.cancel()
        interactionTimeoutJob = coroutineScope.launch {
            delay(INTERACTION_TIMEOUT)

            if (interactionStep == InteractionStep.PAIR_DEVICE) {
                Timber.i("Interaction timed out. Asking user to input pairing code manually.")

                showNotification(
                    title = getString(R.string.pro_mode_setup_notification_pairing_button_not_found_title),
                    text = getString(R.string.pro_mode_setup_notification_pairing_button_not_found_text),
                    actions = listOf(KMNotificationAction.RemoteInput.PairingCode to getString(R.string.pro_mode_setup_notification_action_input_pairing_code))
                )

                // Give the user 30 seconds to input the pairing code and then dismiss the notification.
                delay(30000)
            }

            dismissNotification()

            interactionStep = null
        }
    }

    private fun stopInteracting() {
        interactionStep = null
        interactionTimeoutJob?.cancel()
        interactionTimeoutJob = null
    }

    private fun getKeyMapperAppTask(): ActivityManager.AppTask? {
        val task = activityManager.appTasks
            .firstOrNull { it.taskInfo.topActivity?.className == keyMapperClassProvider.getMainActivity().name }
        return task
    }
}