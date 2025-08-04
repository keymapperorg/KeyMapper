package io.github.sds100.keymapper.base.system.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.lifecycleScope
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.actions.PerformActionsUseCaseImpl
import io.github.sds100.keymapper.base.actions.TestActionEvent
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCaseImpl
import io.github.sds100.keymapper.base.input.InputEventDetectionSource
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.TriggerKeyMapEvent
import io.github.sds100.keymapper.base.keymaps.detection.DetectKeyMapsUseCaseImpl
import io.github.sds100.keymapper.base.keymaps.detection.DetectScreenOffKeyEventsController
import io.github.sds100.keymapper.base.keymaps.detection.DpadMotionEventTracker
import io.github.sds100.keymapper.base.keymaps.detection.KeyMapController
import io.github.sds100.keymapper.base.keymaps.detection.TriggerKeyMapFromOtherAppsController
import io.github.sds100.keymapper.base.reroutekeyevents.RerouteKeyEventsController
import io.github.sds100.keymapper.base.trigger.RecordTriggerEvent
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.common.utils.hasFlag
import io.github.sds100.keymapper.common.utils.minusFlag
import io.github.sds100.keymapper.common.utils.withFlag
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupController
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
import io.github.sds100.keymapper.system.inputevents.KMMotionEvent
import io.github.sds100.keymapper.system.root.SuAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

abstract class BaseAccessibilityServiceController(
    private val service: BaseAccessibilityService,
    private val rerouteKeyEventsControllerFactory: RerouteKeyEventsController.Factory,
    private val accessibilityNodeRecorderFactory: AccessibilityNodeRecorder.Factory,
    private val performActionsUseCaseFactory: PerformActionsUseCaseImpl.Factory,
    private val detectKeyMapsUseCaseFactory: DetectKeyMapsUseCaseImpl.Factory,
    private val detectConstraintsUseCaseFactory: DetectConstraintsUseCaseImpl.Factory,
    private val fingerprintGesturesSupported: FingerprintGesturesSupportedUseCase,
    private val pauseKeyMapsUseCase: PauseKeyMapsUseCase,
    private val devicesAdapter: DevicesAdapter,
    private val suAdapter: SuAdapter,
    private val settingsRepository: PreferenceRepository,
    private val systemBridgeSetupController: SystemBridgeSetupController
) {
    companion object {

        /**
         * How long should the accessibility service record a trigger in seconds.
         */
        private const val RECORD_TRIGGER_TIMER_LENGTH = 5
        private const val DEFAULT_NOTIFICATION_TIMEOUT = 200L
    }

    private val performActionsUseCase = performActionsUseCaseFactory.create(
        accessibilityService = service,
        imeInputEventInjector = service.imeInputEventInjector,
    )

    private val detectKeyMapsUseCase = detectKeyMapsUseCaseFactory.create(
        accessibilityService = service,
        coroutineScope = service.lifecycleScope,
        imeInputEventInjector = service.imeInputEventInjector,
    )

    val detectConstraintsUseCase = detectConstraintsUseCaseFactory.create(service)

    val keyMapController = KeyMapController(
        service.lifecycleScope,
        detectKeyMapsUseCase,
        performActionsUseCase,
        detectConstraintsUseCase,
    )

    val triggerKeyMapFromOtherAppsController = TriggerKeyMapFromOtherAppsController(
        service.lifecycleScope,
        detectKeyMapsUseCase,
        performActionsUseCase,
        detectConstraintsUseCase,
    )

    val rerouteKeyEventsController = rerouteKeyEventsControllerFactory.create(
        service.lifecycleScope,
        service.imeInputEventInjector,
    )

    val accessibilityNodeRecorder = accessibilityNodeRecorderFactory.create(service)

    private val detectScreenOffKeyEventsController = DetectScreenOffKeyEventsController(
        suAdapter,
        devicesAdapter,
    ) { event ->
        if (!isPaused.value) {
            withContext(Dispatchers.Main.immediate) {
                keyMapController.onKeyEvent(event)
            }
        }
    }

    private var recordingTriggerJob: Job? = null

    private val recordingTrigger: Boolean
        get() = recordingTriggerJob != null && recordingTriggerJob?.isActive == true

    private val recordDpadMotionEventTracker: DpadMotionEventTracker =
        DpadMotionEventTracker()

    val isPaused: StateFlow<Boolean> =
        pauseKeyMapsUseCase.isPaused
            .stateIn(service.lifecycleScope, SharingStarted.Eagerly, false)

    private val screenOffTriggersEnabled: StateFlow<Boolean> =
        detectKeyMapsUseCase.detectScreenOffTriggers
            .stateIn(service.lifecycleScope, SharingStarted.Eagerly, false)

    private val changeImeOnInputFocusFlow: StateFlow<Boolean> =
        settingsRepository
            .get(Keys.changeImeOnInputFocus)
            .map { it ?: PreferenceDefaults.CHANGE_IME_ON_INPUT_FOCUS }
            .stateIn(
                service.lifecycleScope,
                SharingStarted.Lazily,
                PreferenceDefaults.CHANGE_IME_ON_INPUT_FOCUS,
            )

    private val initialServiceFlags: Int by lazy {
        var flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            .withFlag(AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS)
            .withFlag(AccessibilityServiceInfo.DEFAULT)
            .withFlag(AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS)
            // This is required for receive TYPE_WINDOWS_CHANGED events so can
            // detect when to show/hide overlays.
            .withFlag(AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS)
            .withFlag(AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = flags.withFlag(AccessibilityServiceInfo.FLAG_ENABLE_ACCESSIBILITY_VOLUME)
        }

        return@lazy flags
    }

    /*
       On some devices the onServiceConnected method is called multiple times throughout the lifecycle of the service.
       The service flags that the controller *expects* will be stored here. Whenever onServiceConnected is called the
       service's flags will to be updated to these. Whenever these change the controller will check if the service is
       bound and then update them in the service.
     */
    private var serviceFlags: MutableStateFlow<Int> = MutableStateFlow(initialServiceFlags)

    /**
     * FEEDBACK_GENERIC is for some reason required on Android 8.0 to get accessibility events.
     */
    private var serviceFeedbackType: MutableStateFlow<Int> =
        MutableStateFlow(AccessibilityServiceInfo.FEEDBACK_GENERIC)

    val serviceEventTypes: MutableStateFlow<Int> =
        MutableStateFlow(AccessibilityEvent.TYPE_WINDOWS_CHANGED)

    private val serviceNotificationTimeout: MutableStateFlow<Long> =
        MutableStateFlow(DEFAULT_NOTIFICATION_TIMEOUT)

    private val inputEvents: SharedFlow<AccessibilityServiceEvent> =
        service.accessibilityServiceAdapter.eventsToService
    private val outputEvents: MutableSharedFlow<AccessibilityServiceEvent> =
        service.accessibilityServiceAdapter.eventReceiver

    init {
        serviceFlags.onEach { flags ->
            // check that it isn't null because this can only be called once the service is bound
            if (service.serviceFlags != null) {
                service.serviceFlags = flags
            }
        }.launchIn(service.lifecycleScope)

        serviceFeedbackType.onEach { feedbackType ->
            // check that it isn't null because this can only be called once the service is bound
            if (service.serviceFeedbackType != null) {
                service.serviceFeedbackType = feedbackType
            }
        }.launchIn(service.lifecycleScope)

        serviceEventTypes.onEach { eventTypes ->
            // check that it isn't null because this can only be called once the service is bound
            if (service.serviceEventTypes != null) {
                service.serviceEventTypes = eventTypes
            }
        }.launchIn(service.lifecycleScope)

        serviceNotificationTimeout.onEach { timeout ->
            // check that it isn't null because this can only be called once the service is bound
            if (service.notificationTimeout != null) {
                service.notificationTimeout = timeout
            }
        }.launchIn(service.lifecycleScope)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            combine(
                detectKeyMapsUseCase.requestFingerprintGestureDetection,
                isPaused,
            ) { request, isPaused ->
                if (request && !isPaused) {
                    requestFingerprintGestureDetection()
                } else {
                    denyFingerprintGestureDetection()
                }
            }.launchIn(service.lifecycleScope)
        }

        pauseKeyMapsUseCase.isPaused.distinctUntilChanged().onEach {
            keyMapController.reset()
            triggerKeyMapFromOtherAppsController.reset()
        }.launchIn(service.lifecycleScope)

        detectKeyMapsUseCase.isScreenOn.onEach { isScreenOn ->
            if (!isScreenOn) {
                if (screenOffTriggersEnabled.value) {
                    detectScreenOffKeyEventsController.startListening(service.lifecycleScope)
                }
            } else {
                detectScreenOffKeyEventsController.stopListening()
            }
        }.launchIn(service.lifecycleScope)

        inputEvents.onEach {
            onEventFromUi(it)
        }.launchIn(service.lifecycleScope)

        service.isKeyboardHidden
            .drop(1) // Don't send it when collecting initially
            .onEach { isHidden ->
                if (isHidden) {
                    outputEvents.emit(AccessibilityServiceEvent.OnHideKeyboardEvent)
                } else {
                    outputEvents.emit(AccessibilityServiceEvent.OnShowKeyboardEvent)
                }
            }.launchIn(service.lifecycleScope)

        combine(
            pauseKeyMapsUseCase.isPaused,
            detectKeyMapsUseCase.allKeyMapList,
        ) { isPaused, keyMaps ->
            val enableAccessibilityVolumeStream: Boolean

            if (isPaused) {
                enableAccessibilityVolumeStream = false
            } else {
                enableAccessibilityVolumeStream = keyMaps.any { model ->
                    model.keyMap.isEnabled && model.keyMap.actionList.any { it.data is ActionData.Sound }
                }
            }

            if (enableAccessibilityVolumeStream) {
                enableAccessibilityVolumeStream()
            } else {
                disableAccessibilityVolumeStream()
            }
        }.launchIn(service.lifecycleScope)

        service.lifecycleScope.launch {
            accessibilityNodeRecorder.recordState.collectLatest { state ->
                outputEvents.emit(RecordAccessibilityNodeEvent.OnRecordNodeStateChanged(state))
            }
        }

        val imeInputFocusEvents =
            AccessibilityEvent.TYPE_VIEW_FOCUSED or AccessibilityEvent.TYPE_VIEW_CLICKED

        val recordNodeEvents =
            AccessibilityEvent.TYPE_VIEW_FOCUSED or AccessibilityEvent.TYPE_VIEW_CLICKED

        service.lifecycleScope.launch {
            combine(
                changeImeOnInputFocusFlow,
                accessibilityNodeRecorder.recordState,
            ) { changeImeOnInputFocus, recordState ->

                serviceEventTypes.update { eventTypes ->
                    var newEventTypes = eventTypes

                    if (!changeImeOnInputFocus && recordState == RecordAccessibilityNodeState.Idle) {
                        newEventTypes =
                            newEventTypes and (imeInputFocusEvents or recordNodeEvents).inv()
                    } else {
                        if (changeImeOnInputFocus) {
                            newEventTypes = newEventTypes or imeInputFocusEvents
                        }

                        if (recordState is RecordAccessibilityNodeState.CountingDown) {
                            newEventTypes = newEventTypes or recordNodeEvents
                        }
                    }

                    newEventTypes
                }

                serviceNotificationTimeout.update {
                    if (recordState is RecordAccessibilityNodeState.CountingDown) {
                        0L
                    } else {
                        DEFAULT_NOTIFICATION_TIMEOUT
                    }
                }
            }.collect()
        }
    }

    open fun onServiceConnected() {
        service.serviceFlags = serviceFlags.value
        service.serviceFeedbackType = serviceFeedbackType.value
        service.serviceEventTypes = serviceEventTypes.value
        service.notificationTimeout = serviceNotificationTimeout.value

        // check if fingerprint gestures are supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isFingerprintGestureRequested =
                serviceFlags.value.hasFlag(AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES)
            requestFingerprintGestureDetection()

            /* Don't update whether fingerprint gesture detection is supported if it has
             * been supported at some point. Just in case the fingerprint reader is being
             * used while this is called. */
            if (fingerprintGesturesSupported.isSupported.firstBlocking() != true) {
                fingerprintGesturesSupported.setSupported(
                    service.isFingerprintGestureDetectionAvailable,
                )
            }

            if (!isFingerprintGestureRequested) {
                denyFingerprintGestureDetection()
            }
        }
    }

    open fun onDestroy() {
        accessibilityNodeRecorder.teardown()
    }

    open fun onConfigurationChanged(newConfig: Configuration) {
    }

    /**
     * Returns an MyKeyEvent which is either the same or more unique
     */
    private fun getUniqueEvent(event: KMKeyEvent): KMKeyEvent {
        // Guard to ignore processing when not applicable
        if (event.keyCode != KeyEvent.KEYCODE_UNKNOWN) return event

        // Don't offset negative values
        val scanCodeOffset: Int = if (event.scanCode >= 0) {
            InputEventUtils.KEYCODE_TO_SCANCODE_OFFSET
        } else {
            0
        }

        val eventProxy = event.copy(
            // Fallback to scanCode when keyCode is unknown as it's typically more unique
            // Add offset to go past possible keyCode values
            keyCode = event.scanCode + scanCodeOffset,
        )

        return eventProxy
    }

    fun onKeyEvent(
        event: KMKeyEvent,
        detectionSource: InputEventDetectionSource = InputEventDetectionSource.ACCESSIBILITY_SERVICE,
    ): Boolean {
        val detailedLogInfo = event.toString()

        if (recordingTrigger) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                Timber.d("Recorded key ${KeyEvent.keyCodeToString(event.keyCode)}, $detailedLogInfo")

                val uniqueEvent: KMKeyEvent = getUniqueEvent(event)

                service.lifecycleScope.launch {
                    outputEvents.emit(
                        RecordTriggerEvent.RecordedTriggerKey(
                            uniqueEvent.keyCode,
                            uniqueEvent.device,
                            detectionSource,
                        ),
                    )
                }
            }

            return true
        }

        if (isPaused.value) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> Timber.d("Down ${KeyEvent.keyCodeToString(event.keyCode)} - not filtering because paused, $detailedLogInfo")
                KeyEvent.ACTION_UP -> Timber.d("Up ${KeyEvent.keyCodeToString(event.keyCode)} - not filtering because paused, $detailedLogInfo")
            }
        } else {
            try {
                var consume: Boolean
                val uniqueEvent: KMKeyEvent = getUniqueEvent(event)

                consume = keyMapController.onKeyEvent(uniqueEvent)

                if (!consume) {
                    consume = rerouteKeyEventsController.onKeyEvent(uniqueEvent)
                }

                when (uniqueEvent.action) {
                    KeyEvent.ACTION_DOWN -> Timber.d("Down ${KeyEvent.keyCodeToString(uniqueEvent.keyCode)} - consumed: $consume, $detailedLogInfo")
                    KeyEvent.ACTION_UP -> Timber.d("Up ${KeyEvent.keyCodeToString(uniqueEvent.keyCode)} - consumed: $consume, $detailedLogInfo")
                }

                return consume
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        return false
    }

    fun onKeyEventFromIme(event: KMKeyEvent): Boolean {
        /*
        Issue #850
        If a volume key is sent while the phone is ringing or in a call
        then that key event must have been relayed by an input method and only an up event
        is sent. This is a restriction in Android. So send a fake DOWN key event as well
        before returning the UP key event.
         */
        if (event.action == KeyEvent.ACTION_UP && (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            onKeyEvent(
                event.copy(action = KeyEvent.ACTION_DOWN),
                detectionSource = InputEventDetectionSource.INPUT_METHOD,
            )
        }

        return onKeyEvent(
            event,
            detectionSource = InputEventDetectionSource.INPUT_METHOD,
        )
    }

    fun onMotionEventFromIme(event: KMMotionEvent): Boolean {
        if (isPaused.value) {
            return false
        }

        if (recordingTrigger) {
            val dpadKeyEvents = recordDpadMotionEventTracker.convertMotionEvent(event)

            var consume = false

            for (keyEvent in dpadKeyEvents) {
                if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                    Timber.d("Recorded motion event ${KeyEvent.keyCodeToString(keyEvent.keyCode)}")

                    service.lifecycleScope.launch {
                        outputEvents.emit(
                            RecordTriggerEvent.RecordedTriggerKey(
                                keyEvent.keyCode,
                                keyEvent.device,
                                InputEventDetectionSource.INPUT_METHOD,
                            ),
                        )
                    }
                }

                // Consume the key event if it is an DOWN or UP.
                consume = true
            }

            if (consume) {
                return true
            }
        }

        try {
            val consume = keyMapController.onMotionEvent(event)

            return consume
        } catch (e: Exception) {
            Timber.e(e)
            return false
        }
    }

    open fun onAccessibilityEvent(event: AccessibilityEvent) {
        accessibilityNodeRecorder.onAccessibilityEvent(event)

        if (changeImeOnInputFocusFlow.value) {
            val focussedNode =
                service.findFocussedNode(AccessibilityNodeInfo.FOCUS_INPUT)

            if (focussedNode?.isEditable == true && focussedNode.isFocused) {
                Timber.d("Got input focus")
                service.lifecycleScope.launch {
                    outputEvents.emit(AccessibilityServiceEvent.OnInputFocusChange(isFocussed = true))
                }
            } else {
                Timber.d("Lost input focus")
                service.lifecycleScope.launch {
                    outputEvents.emit(AccessibilityServiceEvent.OnInputFocusChange(isFocussed = false))
                }
            }
        }

        // TODO only run this code, and listen for these events if searching for a pairing code. Check that package name is settings app.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            val pairingCodeRegex = Regex("^\\d{6}$")
            val portRegex = Regex(".*:([0-9]{1,5})")
            val pairingCodeNode =
                service.rootInActiveWindow.findNodeRecursively {
                    it.text != null && pairingCodeRegex.matches(it.text)
                }

            val portNode = service.rootInActiveWindow.findNodeRecursively {
                it.text != null && portRegex.matches(it.text)
            }

            if (pairingCodeNode != null && portNode != null) {
                val pairingCode = pairingCodeNode.text?.toString()?.toIntOrNull()
                val port = portNode.text?.split(":")?.last()?.toIntOrNull()
                Timber.e("PAIRING CODE = $pairingCode")
                Timber.e("PORT = $port")

                if (pairingCode != null && port != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        service.lifecycleScope.launch {
                            systemBridgeSetupController.pairWirelessAdb(port, pairingCode)
                            delay(1000)
                            systemBridgeSetupController.startWithAdb()
                        }
                    }
                }
            }
        }
    }

    fun onFingerprintGesture(type: FingerprintGestureType) {
        keyMapController.onFingerprintGesture(type)
    }

    private fun triggerKeyMapFromIntent(uid: String) {
        triggerKeyMapFromOtherAppsController.onDetected(uid)
    }

    open fun onEventFromUi(event: AccessibilityServiceEvent) {
        Timber.d("Service received event from UI: $event")
        when (event) {
            is RecordTriggerEvent.StartRecordingTrigger ->
                if (!recordingTrigger) {
                    recordDpadMotionEventTracker.reset()
                    recordingTriggerJob = recordTriggerJob()
                }

            is RecordTriggerEvent.StopRecordingTrigger -> {
                val wasRecordingTrigger = recordingTrigger

                recordingTriggerJob?.cancel()
                recordingTriggerJob = null
                recordDpadMotionEventTracker.reset()

                if (wasRecordingTrigger) {
                    service.lifecycleScope.launch {
                        outputEvents.emit(RecordTriggerEvent.OnStoppedRecordingTrigger)
                    }
                }
            }

            is TestActionEvent -> service.lifecycleScope.launch {
                performActionsUseCase.perform(
                    event.action,
                )
            }

            is AccessibilityServiceEvent.Ping -> service.lifecycleScope.launch {
                outputEvents.emit(AccessibilityServiceEvent.Pong(event.key))
            }

            is AccessibilityServiceEvent.HideKeyboard -> service.hideKeyboard()
            is AccessibilityServiceEvent.ShowKeyboard -> service.showKeyboard()
            is AccessibilityServiceEvent.ChangeIme -> service.switchIme(event.imeId)
            is AccessibilityServiceEvent.DisableService -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                service.disableSelf()
            }

            is TriggerKeyMapEvent -> triggerKeyMapFromIntent(event.uid)

            is AccessibilityServiceEvent.EnableInputMethod -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                service.setInputMethodEnabled(event.imeId, true)
            }

            is RecordAccessibilityNodeEvent.StartRecordingNodes -> {
                accessibilityNodeRecorder.startRecording()
            }

            is RecordAccessibilityNodeEvent.StopRecordingNodes -> {
                accessibilityNodeRecorder.stopRecording()
            }

            else -> Unit
        }
    }

    private fun recordTriggerJob() = service.lifecycleScope.launch {
        repeat(RECORD_TRIGGER_TIMER_LENGTH) { iteration ->
            if (isActive) {
                val timeLeft = RECORD_TRIGGER_TIMER_LENGTH - iteration
                outputEvents.emit(RecordTriggerEvent.OnIncrementRecordTriggerTimer(timeLeft))

                delay(1000)
            }
        }

        outputEvents.emit(RecordTriggerEvent.OnStoppedRecordingTrigger)
    }

    private fun requestFingerprintGestureDetection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.d("Accessibility service: request fingerprint gesture detection")
            serviceFlags.value =
                serviceFlags.value.withFlag(AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES)
        }
    }

    private fun denyFingerprintGestureDetection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.d("Accessibility service: deny fingerprint gesture detection")
            serviceFlags.value =
                serviceFlags.value.minusFlag(AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES)
        }
    }

    private fun enableAccessibilityVolumeStream() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceFeedbackType.value =
                serviceFeedbackType.value.withFlag(AccessibilityServiceInfo.FEEDBACK_AUDIBLE)
            serviceFlags.value =
                serviceFlags.value.withFlag(AccessibilityServiceInfo.FLAG_ENABLE_ACCESSIBILITY_VOLUME)
        }
    }

    private fun disableAccessibilityVolumeStream() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceFeedbackType.value =
                serviceFeedbackType.value.minusFlag(AccessibilityServiceInfo.FEEDBACK_AUDIBLE)
            serviceFlags.value =
                serviceFlags.value.minusFlag(AccessibilityServiceInfo.FLAG_ENABLE_ACCESSIBILITY_VOLUME)
        }
    }
}
