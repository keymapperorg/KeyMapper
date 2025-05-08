package io.github.sds100.keymapper.system.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.AccessibilityNodeRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.FingerprintGestureType
import io.github.sds100.keymapper.mappings.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.mappings.PauseKeyMapsUseCase
import io.github.sds100.keymapper.mappings.keymaps.detection.DetectKeyMapsUseCase
import io.github.sds100.keymapper.mappings.keymaps.detection.DetectScreenOffKeyEventsController
import io.github.sds100.keymapper.mappings.keymaps.detection.DpadMotionEventTracker
import io.github.sds100.keymapper.mappings.keymaps.detection.KeyMapController
import io.github.sds100.keymapper.mappings.keymaps.detection.TriggerKeyMapFromOtherAppsController
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyEventDetectionSource
import io.github.sds100.keymapper.reroutekeyevents.RerouteKeyEventsController
import io.github.sds100.keymapper.reroutekeyevents.RerouteKeyEventsUseCase
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.system.inputevents.MyKeyEvent
import io.github.sds100.keymapper.system.inputevents.MyMotionEvent
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.util.ServiceEvent
import io.github.sds100.keymapper.util.firstBlocking
import kotlinx.coroutines.CoroutineScope
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
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag
import timber.log.Timber

/**
 * Created by sds100 on 17/04/2021.
 */
abstract class BaseAccessibilityServiceController(
    private val coroutineScope: CoroutineScope,
    private val service: MyAccessibilityService,
    private val inputEvents: SharedFlow<ServiceEvent>,
    private val outputEvents: MutableSharedFlow<ServiceEvent>,
    private val detectConstraintsUseCase: DetectConstraintsUseCase,
    private val performActionsUseCase: PerformActionsUseCase,
    private val detectKeyMapsUseCase: DetectKeyMapsUseCase,
    private val fingerprintGesturesSupported: FingerprintGesturesSupportedUseCase,
    rerouteKeyEventsUseCase: RerouteKeyEventsUseCase,
    private val pauseKeyMapsUseCase: PauseKeyMapsUseCase,
    private val devicesAdapter: DevicesAdapter,
    private val suAdapter: SuAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val settingsRepository: PreferenceRepository,
    private val nodeRepository: AccessibilityNodeRepository,
) {

    companion object {
        /**
         * How long should the accessibility service record a trigger in seconds.
         */
        private const val RECORD_TRIGGER_TIMER_LENGTH = 5

        private const val DEFAULT_NOTIFICATION_TIMEOUT = 200L
    }

    private val triggerKeyMapFromOtherAppsController = TriggerKeyMapFromOtherAppsController(
        coroutineScope,
        detectKeyMapsUseCase,
        performActionsUseCase,
        detectConstraintsUseCase,
    )

    val keyMapController = KeyMapController(
        coroutineScope,
        detectKeyMapsUseCase,
        performActionsUseCase,
        detectConstraintsUseCase,
    )

    private val rerouteKeyEventsController = RerouteKeyEventsController(
        coroutineScope,
        rerouteKeyEventsUseCase,
    )

    private val accessibilityNodeRecorder: AccessibilityNodeRecorder =
        AccessibilityNodeRecorder(nodeRepository, service)

    private var recordingTriggerJob: Job? = null
    private val recordingTrigger: Boolean
        get() = recordingTriggerJob != null && recordingTriggerJob?.isActive == true
    private val recordDpadMotionEventTracker: DpadMotionEventTracker =
        DpadMotionEventTracker()

    val isPaused: StateFlow<Boolean> = pauseKeyMapsUseCase.isPaused
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    private val screenOffTriggersEnabled: StateFlow<Boolean> =
        detectKeyMapsUseCase.detectScreenOffTriggers
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    private val changeImeOnInputFocusFlow: StateFlow<Boolean> =
        settingsRepository
            .get(Keys.changeImeOnInputFocus)
            .map { it ?: PreferenceDefaults.CHANGE_IME_ON_INPUT_FOCUS }
            .stateIn(
                coroutineScope,
                SharingStarted.Lazily,
                PreferenceDefaults.CHANGE_IME_ON_INPUT_FOCUS,
            )

    private val detectScreenOffKeyEventsController =
        DetectScreenOffKeyEventsController(
            suAdapter,
            devicesAdapter,
        ) { event ->

            if (!isPaused.value) {
                withContext(Dispatchers.Main.immediate) {
                    keyMapController.onKeyEvent(event)
                }
            }
        }

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

    init {

        serviceFlags.onEach { flags ->
            // check that it isn't null because this can only be called once the service is bound
            if (service.serviceFlags != null) {
                service.serviceFlags = flags
            }
        }.launchIn(coroutineScope)

        serviceFeedbackType.onEach { feedbackType ->
            // check that it isn't null because this can only be called once the service is bound
            if (service.serviceFeedbackType != null) {
                service.serviceFeedbackType = feedbackType
            }
        }.launchIn(coroutineScope)

        serviceEventTypes.onEach { eventTypes ->
            // check that it isn't null because this can only be called once the service is bound
            if (service.serviceEventTypes != null) {
                service.serviceEventTypes = eventTypes
            }
        }.launchIn(coroutineScope)

        serviceNotificationTimeout.onEach { timeout ->
            // check that it isn't null because this can only be called once the service is bound
            if (service.notificationTimeout != null) {
                service.notificationTimeout = timeout
            }
        }.launchIn(coroutineScope)

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
            }.launchIn(coroutineScope)
        }

        pauseKeyMapsUseCase.isPaused.distinctUntilChanged().onEach {
            keyMapController.reset()
            triggerKeyMapFromOtherAppsController.reset()
        }.launchIn(coroutineScope)

        detectKeyMapsUseCase.isScreenOn.onEach { isScreenOn ->
            if (!isScreenOn) {
                if (screenOffTriggersEnabled.value) {
                    detectScreenOffKeyEventsController.startListening(coroutineScope)
                }
            } else {
                detectScreenOffKeyEventsController.stopListening()
            }
        }.launchIn(coroutineScope)

        inputEvents.onEach {
            onEventFromUi(it)
        }.launchIn(coroutineScope)

        service.isKeyboardHidden
            .drop(1) // Don't send it when collecting initially
            .onEach { isHidden ->
                if (isHidden) {
                    outputEvents.emit(ServiceEvent.OnHideKeyboardEvent)
                } else {
                    outputEvents.emit(ServiceEvent.OnShowKeyboardEvent)
                }
            }.launchIn(coroutineScope)

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
        }.launchIn(coroutineScope)

        coroutineScope.launch {
            accessibilityNodeRecorder.recordState.collectLatest { state ->
                outputEvents.emit(ServiceEvent.OnRecordNodeStateChanged(state))
            }
        }

        val imeInputFocusEvents =
            AccessibilityEvent.TYPE_VIEW_FOCUSED or AccessibilityEvent.TYPE_VIEW_CLICKED

        // Listen to WINDOWS_CHANGED event in case no events are received when the user
        // interacts directly with elements.
        val recordNodeEvents = AccessibilityEvent.TYPE_VIEW_FOCUSED or
            AccessibilityEvent.TYPE_VIEW_CLICKED or
            AccessibilityEvent.TYPE_WINDOWS_CHANGED

        coroutineScope.launch {
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
    private fun getUniqueEvent(event: MyKeyEvent): MyKeyEvent {
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
        event: MyKeyEvent,
        detectionSource: KeyEventDetectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
    ): Boolean {
        val detailedLogInfo = event.toString()

        if (recordingTrigger) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                Timber.d("Recorded key ${KeyEvent.keyCodeToString(event.keyCode)}, $detailedLogInfo")

                val uniqueEvent: MyKeyEvent = getUniqueEvent(event)

                coroutineScope.launch {
                    outputEvents.emit(
                        ServiceEvent.RecordedTriggerKey(
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
                val uniqueEvent: MyKeyEvent = getUniqueEvent(event)

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

    fun onKeyEventFromIme(event: MyKeyEvent): Boolean {
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
                detectionSource = KeyEventDetectionSource.INPUT_METHOD,
            )
        }

        return onKeyEvent(
            event,
            detectionSource = KeyEventDetectionSource.INPUT_METHOD,
        )
    }

    fun onMotionEventFromIme(event: MyMotionEvent): Boolean {
        if (isPaused.value) {
            return false
        }

        if (recordingTrigger) {
            val dpadKeyEvents = recordDpadMotionEventTracker.convertMotionEvent(event)

            var consume = false

            for (keyEvent in dpadKeyEvents) {
                if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                    Timber.d("Recorded motion event ${KeyEvent.keyCodeToString(keyEvent.keyCode)}")

                    coroutineScope.launch {
                        outputEvents.emit(
                            ServiceEvent.RecordedTriggerKey(
                                keyEvent.keyCode,
                                keyEvent.device,
                                KeyEventDetectionSource.INPUT_METHOD,
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
                coroutineScope.launch {
                    outputEvents.emit(ServiceEvent.OnInputFocusChange(isFocussed = true))
                }
            } else {
                Timber.d("Lost input focus")
                coroutineScope.launch {
                    outputEvents.emit(ServiceEvent.OnInputFocusChange(isFocussed = false))
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

    open fun onEventFromUi(event: ServiceEvent) {
        Timber.d("Service received event from UI: $event")
        when (event) {
            is ServiceEvent.StartRecordingTrigger ->
                if (!recordingTrigger) {
                    recordDpadMotionEventTracker.reset()
                    recordingTriggerJob = recordTriggerJob()
                }

            is ServiceEvent.StopRecordingTrigger -> {
                val wasRecordingTrigger = recordingTrigger

                recordingTriggerJob?.cancel()
                recordingTriggerJob = null
                recordDpadMotionEventTracker.reset()

                if (wasRecordingTrigger) {
                    coroutineScope.launch {
                        outputEvents.emit(ServiceEvent.OnStoppedRecordingTrigger)
                    }
                }
            }

            is ServiceEvent.TestAction -> coroutineScope.launch {
                performActionsUseCase.perform(
                    event.action,
                )
            }

            is ServiceEvent.Ping -> coroutineScope.launch {
                outputEvents.emit(ServiceEvent.Pong(event.key))
            }

            is ServiceEvent.HideKeyboard -> service.hideKeyboard()
            is ServiceEvent.ShowKeyboard -> service.showKeyboard()
            is ServiceEvent.ChangeIme -> service.switchIme(event.imeId)
            is ServiceEvent.DisableService -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                service.disableSelf()
            }

            is ServiceEvent.TriggerKeyMap -> triggerKeyMapFromIntent(event.uid)

            is ServiceEvent.EnableInputMethod -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                service.setInputMethodEnabled(event.imeId, true)
            }

            is ServiceEvent.StartRecordingNodes -> {
                accessibilityNodeRecorder.startRecording()
            }

            is ServiceEvent.StopRecordingNodes -> {
                accessibilityNodeRecorder.stopRecording()
            }

            else -> Unit
        }
    }

    private fun recordTriggerJob() = coroutineScope.launch {
        repeat(RECORD_TRIGGER_TIMER_LENGTH) { iteration ->
            if (isActive) {
                val timeLeft = RECORD_TRIGGER_TIMER_LENGTH - iteration
                outputEvents.emit(ServiceEvent.OnIncrementRecordTriggerTimer(timeLeft))

                delay(1000)
            }
        }

        outputEvents.emit(ServiceEvent.OnStoppedRecordingTrigger)
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
