package io.github.sds100.keymapper.system.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.PauseMappingsUseCase
import io.github.sds100.keymapper.mappings.fingerprintmaps.DetectFingerprintMapsUseCase
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintGestureMapController
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapId
import io.github.sds100.keymapper.mappings.keymaps.detection.DetectKeyMapsUseCase
import io.github.sds100.keymapper.mappings.keymaps.detection.DetectScreenOffKeyEventsController
import io.github.sds100.keymapper.mappings.keymaps.detection.DpadMotionEventTracker
import io.github.sds100.keymapper.mappings.keymaps.detection.KeyMapController
import io.github.sds100.keymapper.mappings.keymaps.detection.TriggerKeyMapFromOtherAppsController
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyEventDetectionSource
import io.github.sds100.keymapper.reroutekeyevents.RerouteKeyEventsController
import io.github.sds100.keymapper.reroutekeyevents.RerouteKeyEventsUseCase
import io.github.sds100.keymapper.system.devices.DevicesAdapter
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
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
    private val accessibilityService: IAccessibilityService,
    private val inputEvents: SharedFlow<ServiceEvent>,
    private val outputEvents: MutableSharedFlow<ServiceEvent>,
    private val detectConstraintsUseCase: DetectConstraintsUseCase,
    private val performActionsUseCase: PerformActionsUseCase,
    private val detectKeyMapsUseCase: DetectKeyMapsUseCase,
    private val detectFingerprintMapsUseCase: DetectFingerprintMapsUseCase,
    rerouteKeyEventsUseCase: RerouteKeyEventsUseCase,
    private val pauseMappingsUseCase: PauseMappingsUseCase,
    private val devicesAdapter: DevicesAdapter,
    private val suAdapter: SuAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val settingsRepository: PreferenceRepository,
) {

    companion object {
        /**
         * How long should the accessibility service record a trigger in seconds.
         */
        private const val RECORD_TRIGGER_TIMER_LENGTH = 5
    }

    private val triggerKeyMapFromOtherAppsController = TriggerKeyMapFromOtherAppsController(
        coroutineScope,
        detectKeyMapsUseCase,
        performActionsUseCase,
        detectConstraintsUseCase,
    )

    private val fingerprintMapController = FingerprintGestureMapController(
        coroutineScope,
        detectFingerprintMapsUseCase,
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

    private var recordingTriggerJob: Job? = null
    private val recordingTrigger: Boolean
        get() = recordingTriggerJob != null && recordingTriggerJob?.isActive == true
    private val recordDpadMotionEventTracker: DpadMotionEventTracker =
        DpadMotionEventTracker()

    val isPaused: StateFlow<Boolean> = pauseMappingsUseCase.isPaused
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    private val screenOffTriggersEnabled: StateFlow<Boolean> =
        detectKeyMapsUseCase.detectScreenOffTriggers
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

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

    private val initialEventTypes: Int = AccessibilityEvent.TYPE_WINDOWS_CHANGED
    private var serviceEventTypes: MutableStateFlow<Int> = MutableStateFlow(initialEventTypes)

    init {
        serviceFlags.onEach { flags ->
            // check that it isn't null because this can only be called once the service is bound
            if (accessibilityService.serviceFlags != null) {
                accessibilityService.serviceFlags = flags
            }
        }.launchIn(coroutineScope)

        serviceFeedbackType.onEach { feedbackType ->
            // check that it isn't null because this can only be called once the service is bound
            if (accessibilityService.serviceFeedbackType != null) {
                accessibilityService.serviceFeedbackType = feedbackType
            }
        }.launchIn(coroutineScope)

        serviceEventTypes.onEach { eventTypes ->
            // check that it isn't null because this can only be called once the service is bound
            if (accessibilityService.serviceEventTypes != null) {
                accessibilityService.serviceEventTypes = eventTypes
            }
        }.launchIn(coroutineScope)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            combine(
                detectFingerprintMapsUseCase.fingerprintMaps,
                isPaused,
            ) { fingerprintMaps, isPaused ->
                if (fingerprintMaps.toList()
                        .any { it.isEnabled && it.actionList.isNotEmpty() } &&
                    !isPaused
                ) {
                    requestFingerprintGestureDetection()
                } else {
                    denyFingerprintGestureDetection()
                }
            }.launchIn(coroutineScope)
        }

        pauseMappingsUseCase.isPaused.distinctUntilChanged().onEach {
            keyMapController.reset()
            fingerprintMapController.reset()
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

        accessibilityService.isKeyboardHidden
            .drop(1) // Don't send it when collecting initially
            .onEach { isHidden ->
                if (isHidden) {
                    outputEvents.emit(ServiceEvent.OnHideKeyboardEvent)
                } else {
                    outputEvents.emit(ServiceEvent.OnShowKeyboardEvent)
                }
            }.launchIn(coroutineScope)

        combine(
            pauseMappingsUseCase.isPaused,
            merge(detectKeyMapsUseCase.allKeyMapList, detectFingerprintMapsUseCase.fingerprintMaps),
        ) { isPaused, mappings ->
            val enableAccessibilityVolumeStream: Boolean

            if (isPaused) {
                enableAccessibilityVolumeStream = false
            } else {
                enableAccessibilityVolumeStream = mappings.any { mapping ->
                    mapping.isEnabled && mapping.actionList.any { it.data is ActionData.Sound }
                }
            }

            if (enableAccessibilityVolumeStream) {
                enableAccessibilityVolumeStream()
            } else {
                disableAccessibilityVolumeStream()
            }
        }.launchIn(coroutineScope)

        settingsRepository.get(Keys.changeImeOnInputFocus).onEach { changeImeOnInputFocus ->
            if (changeImeOnInputFocus == true) {
                serviceEventTypes.value = serviceEventTypes.value
                    .withFlag(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    .withFlag(AccessibilityEvent.TYPE_VIEW_CLICKED)
            } else {
                serviceEventTypes.value = serviceEventTypes.value
                    .minusFlag(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    .minusFlag(AccessibilityEvent.TYPE_VIEW_CLICKED)
            }
        }.launchIn(coroutineScope)
    }

    fun onServiceConnected() {
        accessibilityService.serviceFlags = serviceFlags.value
        accessibilityService.serviceFeedbackType = serviceFeedbackType.value
        accessibilityService.serviceEventTypes = serviceEventTypes.value

        // check if fingerprint gestures are supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isFingerprintGestureRequested =
                serviceFlags.value.hasFlag(AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES)
            requestFingerprintGestureDetection()

            /* Don't update whether fingerprint gesture detection is supported if it has
             * been supported at some point. Just in case the fingerprint reader is being
             * used while this is called. */
            if (detectFingerprintMapsUseCase.isSupported.firstBlocking() != true) {
                detectFingerprintMapsUseCase.setSupported(
                    accessibilityService.isFingerprintGestureDetectionAvailable,
                )
            }

            if (!isFingerprintGestureRequested) {
                denyFingerprintGestureDetection()
            }
        }
    }

    fun onKeyEvent(
        event: MyKeyEvent,
        detectionSource: KeyEventDetectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
    ): Boolean {
        val detailedLogInfo = event.toString()

        if (recordingTrigger) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                Timber.d("Recorded key ${KeyEvent.keyCodeToString(event.keyCode)}, $detailedLogInfo")
                coroutineScope.launch {
                    outputEvents.emit(
                        ServiceEvent.RecordedTriggerKey(
                            event.keyCode,
                            event.device,
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

                consume = keyMapController.onKeyEvent(event)

                if (!consume) {
                    consume = rerouteKeyEventsController.onKeyEvent(event)
                }

                when (event.action) {
                    KeyEvent.ACTION_DOWN -> Timber.d("Down ${KeyEvent.keyCodeToString(event.keyCode)} - consumed: $consume, $detailedLogInfo")
                    KeyEvent.ACTION_UP -> Timber.d("Up ${KeyEvent.keyCodeToString(event.keyCode)} - consumed: $consume, $detailedLogInfo")
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

    fun onAccessibilityEvent(event: AccessibilityEventModel) {
        Timber.d("OnAccessibilityEvent $event")
        val focussedNode = accessibilityService.findFocussedNode(AccessibilityNodeInfo.FOCUS_INPUT)

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

    fun onFingerprintGesture(id: FingerprintMapId) {
        fingerprintMapController.onGesture(id)
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

            is ServiceEvent.TestAction -> performActionsUseCase.perform(event.action)

            is ServiceEvent.Ping -> coroutineScope.launch {
                outputEvents.emit(ServiceEvent.Pong(event.key))
            }

            is ServiceEvent.HideKeyboard -> accessibilityService.hideKeyboard()
            is ServiceEvent.ShowKeyboard -> accessibilityService.showKeyboard()
            is ServiceEvent.ChangeIme -> accessibilityService.switchIme(event.imeId)
            is ServiceEvent.DisableService -> accessibilityService.disableSelf()

            is ServiceEvent.TriggerKeyMap -> triggerKeyMapFromIntent(event.uid)
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
