package io.github.sds100.keymapper.base.expertmode

import android.app.ActivityManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.sds100.keymapper.base.BaseMainActivity
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.expertmode.SystemBridgeSetupAssistantController.Companion.PAIRING_CODE_BUTTON_STRING_RES_NAMES
import io.github.sds100.keymapper.base.system.accessibility.BaseAccessibilityService
import io.github.sds100.keymapper.base.system.accessibility.findActionTarget
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
import io.github.sds100.keymapper.sysbridge.manager.awaitConnected
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupController
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupStep
import io.github.sds100.keymapper.system.notifications.NotificationModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

@Suppress("KotlinConstantConditions")
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
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider {
    @AssistedFactory
    interface Factory {
        fun create(
            coroutineScope: CoroutineScope,
            accessibilityService: BaseAccessibilityService,
        ): SystemBridgeSetupAssistantController
    }

    companion object {
        /**
         * The max time to spend searching for an accessibility node.
         */
        const val INTERACTION_TIMEOUT = 10000L

        private val PAIRING_CODE_REGEX = Regex("^\\d{6}$")
        private val IPV4_REGEX =
            Regex(
                "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
            )

        private const val SETTINGS_PACKAGE = "com.android.settings"

        /**
         * The names of the string resources in the Settings app for the button that
         * starts pairing with a code. Reading them from the Settings app means this
         * works in every language.
         */
        private val PAIRING_CODE_BUTTON_STRING_RES_NAMES = arrayOf(
            "adb_pair_method_code_title",
            "adb_pair_method_code_summary",
        )

        /**
         * A fallback for when the Settings app on this device doesn't have the
         * resources in [PAIRING_CODE_BUTTON_STRING_RES_NAMES].
         */
        private val PAIRING_CODE_BUTTON_TEXT_FILTER = arrayOf(
            "six-digit code", // English
            "six digit code", // English
            "kode enam digit", // Indonesian
            "código de seis dígitos", // Spanish (US) and Portuguese (Brazil)
            "छह अंकों वाला कोड इस्तेमाल", // Hindi (India)
            "шестизначный код", // Russian (Russia)
            "من 6 أعداد", // Arabic (Egypt)
        )
    }

    private enum class InteractionStep {
        // Do not automatically turn on the wireless debugging switch. When the user turns it on,
        // Key Mapper will automatically pair.
        PAIR_DEVICE,
    }

    private val activityManager: ActivityManager = accessibilityService.getSystemService()!!

    private val isInteractive: StateFlow<Boolean> =
        preferenceRepository.get(Keys.isExpertModeInteractiveSetupAssistantEnabled)
            .map { it ?: PreferenceDefaults.EXPERT_MODE_INTERACTIVE_SETUP_ASSISTANT }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                PreferenceDefaults.EXPERT_MODE_INTERACTIVE_SETUP_ASSISTANT,
            )

    private var interactionStep: InteractionStep? = null

    /**
     * This job will wait for the interaction timeout and then
     * ask the user to do the steps manually if it failed to do them automatically.
     */
    private var interactionTimeoutJob: Job? = null

    // Store the pairing code so only one request to pair is sent per pairing code.
    private var foundPairingCode: String? = null

    /**
     * The text on the button that starts pairing with a code. Read the strings from the
     * Settings app itself so this works in every language, and fall back to a hardcoded
     * list of translations if this device doesn't have those resources.
     */
    private val pairingCodeButtonTextFilter: List<String> by lazy {
        getPairingCodeButtonStrings()
    }

    fun onServiceConnected() {
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
                    .filter {
                        it.intentAction ==
                            KMNotificationAction.IntentAction.PAIRING_CODE_REPLY
                    }
                    .collect { textInput ->
                        Timber.i("Receive pairing code text input: $textInput")

                        val pairingCode: String = textInput.text.trim()
                        onPairingCodeFound(pairingCode)
                    }
            }
        }
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

            if (rootNode.packageName != SETTINGS_PACKAGE) {
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

        Timber.i("Pairing code found $pairingCode. Pairing ADB...")
        setupController.pairWirelessAdb(pairingCode).onSuccess {
            onPairingSuccess()
        }.onFailure {
            Timber.e("Failed to pair with wireless ADB: $it")
            stopInteracting()

            showNotification(
                getString(R.string.expert_mode_setup_notification_invalid_pairing_code_title),
                getString(R.string.expert_mode_setup_notification_invalid_pairing_code_text),
                actions = listOf(
                    KMNotificationAction.RemoteInput.PairingCode to
                        getString(
                            R.string.expert_mode_setup_notification_action_input_pairing_code,
                        ),
                ),
            )
        }
    }

    private suspend fun onPairingSuccess() {
        setupController.startWithAdb()

        val isStarted = withTimeoutOrNull(10000L) {
            systemBridgeConnectionManager.awaitConnected()
        } != null

        if (isStarted) {
            Timber.i("System bridge started after pairing. Going back to Key Mapper.")
            getKeyMapperAppTask()?.moveToFront()
        } else {
            Timber.e("Failed to start system bridge after pairing.")
            showNotification(
                getString(R.string.expert_mode_setup_notification_start_system_bridge_failed_title),
                getString(R.string.expert_mode_setup_notification_start_system_bridge_failed_text),
                onClickAction = KMNotificationAction.Activity.MainActivity(
                    BaseMainActivity.ACTION_START_SYSTEM_BRIDGE,
                ),
            )
        }

        stopInteracting()
    }

    private fun clickPairWithCodeButton(rootNode: AccessibilityNodeInfo) {
        val textNode = rootNode.findNodeRecursively { node ->
            pairingCodeButtonTextFilter.any { text -> node.text?.contains(text) == true }
        } ?: return

        // The node with the text is almost never the node that handles the click. It is
        // usually a child of the row that handles it, and on some ROMs the clickable node
        // is a sibling covering the same place on screen. Resolving the node by its
        // position is more maintainable than assuming where it is in the tree because
        // that can change subtly between Android devices and ROMs.
        val targetNode =
            textNode.findActionTarget(action = AccessibilityNodeInfo.ACTION_CLICK)

        if (targetNode == null) {
            Timber.w("Found the pair with code text but no node that can click it.")
            return
        }

        val success = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Timber.i("Clicked the pair with code button: $success")
    }

    private fun showNotification(
        title: String,
        text: String,
        onClickAction: KMNotificationAction? = null,
        actions: List<Pair<KMNotificationAction, String>> = emptyList(),
    ) {
        val notification = NotificationModel(
            // Use the same notification id for all so they overwrite each other.
            id = NotificationController.Companion.ID_SETUP_ASSISTANT,
            channel = NotificationController.Companion.CHANNEL_SETUP_ASSISTANT,
            title = title,
            text = text,
            icon = R.drawable.offline_bolt_24px,
            onGoing = false,
            showOnLockscreen = false,
            autoCancel = true,
            onClickAction = onClickAction,
            bigTextStyle = true,
            // Must not be silent so it is shown as a heads up notification
            silent = false,
            actions = actions,
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
                    getString(R.string.expert_mode_setup_notification_tap_build_number_title),
                    getString(R.string.expert_mode_setup_notification_tap_build_number_text),
                )
            }

            SystemBridgeSetupStep.ADB_PAIRING -> {
                if (isInteractive.value) {
                    showNotification(
                        getString(R.string.expert_mode_setup_notification_pairing_title),
                        getString(R.string.expert_mode_setup_notification_pairing_text),
                    )
                    interactionStep = InteractionStep.PAIR_DEVICE
                    startInteractionTimeoutJob()
                } else {
                    // Show a notification asking for pairing code straight away if interaction
                    // is disabled.
                    nonInteractivePairingCodeNotification()

                    interactionStep = null
                }
            }

            else -> return
        }
    }

    private fun nonInteractivePairingCodeNotification() {
        showNotification(
            title = getString(
                R.string.expert_mode_setup_notification_pairing_code_not_interactive_title,
            ),
            text = getString(
                R.string.expert_mode_setup_notification_pairing_code_not_interactive_text,
            ),
            actions = listOf(
                KMNotificationAction.RemoteInput.PairingCode to
                    getString(
                        R.string.expert_mode_setup_notification_action_input_pairing_code,
                    ),
            ),
        )
    }

    private fun startInteractionTimeoutJob() {
        interactionTimeoutJob?.cancel()
        interactionTimeoutJob = coroutineScope.launch {
            delay(INTERACTION_TIMEOUT)

            if (interactionStep == InteractionStep.PAIR_DEVICE) {
                Timber.i("Interaction timed out. Asking user to input pairing code manually.")

                showNotification(
                    title = getString(
                        R.string.expert_mode_setup_notification_pairing_button_not_found_title,
                    ),
                    text = getString(
                        R.string.expert_mode_setup_notification_pairing_button_not_found_text,
                    ),
                    actions = listOf(
                        KMNotificationAction.RemoteInput.PairingCode to
                            getString(
                                R.string.expert_mode_setup_notification_action_input_pairing_code,
                            ),
                    ),
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
            ?.firstOrNull {
                it.taskInfo?.topActivity?.className ==
                    keyMapperClassProvider.getMainActivity().name
            }

        return task
    }

    private fun getPairingCodeButtonStrings(): List<String> {
        val stringsFromSettings = try {
            val resources = accessibilityService.packageManager
                .getResourcesForApplication(SETTINGS_PACKAGE)

            PAIRING_CODE_BUTTON_STRING_RES_NAMES.mapNotNull { name ->
                val id = resources.getIdentifier(name, "string", SETTINGS_PACKAGE)

                if (id == 0) {
                    null
                } else {
                    resources.getString(id).takeIf { it.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to read the pairing strings from $SETTINGS_PACKAGE")
            emptyList()
        }

        Timber.d("Pairing button strings from Settings: $stringsFromSettings")

        return stringsFromSettings + PAIRING_CODE_BUTTON_TEXT_FILTER
    }
}
