package io.github.sds100.keymapper.system.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.sds100.keymapper.BuildConfig
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.entities.ViewIdEntity
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.data.repositories.ViewIdRepository
import io.github.sds100.keymapper.mappings.PauseMappingsUseCase
import io.github.sds100.keymapper.mappings.fingerprintmaps.DetectFingerprintMapsUseCase
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintGestureMapController
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapId
import io.github.sds100.keymapper.mappings.keymaps.TriggerKeyMapFromOtherAppsController
import io.github.sds100.keymapper.mappings.keymaps.detection.DetectKeyMapsUseCase
import io.github.sds100.keymapper.mappings.keymaps.detection.DetectScreenOffKeyEventsController
import io.github.sds100.keymapper.mappings.keymaps.detection.KeyMapController
import io.github.sds100.keymapper.reroutekeyevents.RerouteKeyEventsController
import io.github.sds100.keymapper.reroutekeyevents.RerouteKeyEventsUseCase
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.firstBlocking
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag
import timber.log.Timber

/**
 * Created by sds100 on 17/04/2021.
 */
class AccessibilityServiceController(
    private val coroutineScope: CoroutineScope,
    private val accessibilityService: IAccessibilityService,
    private val inputEvents: SharedFlow<Event>,
    private val outputEvents: MutableSharedFlow<Event>,
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
    private val viewIdRepository: ViewIdRepository
) {

    companion object {
        /**
         * How long should the accessibility service record a trigger in seconds.
         */
        private const val RECORD_TRIGGER_TIMER_LENGTH = 5

        /**
         * How long should the accessibility service record UI elements in seconds
         */
        private const val RECORD_UI_ELEMENTS_TIMER_LENGTH = 5 * 60
        /**
         * On which events we want to record UI Elements?
         */
        private val RECORD_UI_ELEMENTS_EVENT_TYPES = intArrayOf(
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        )
    }

    private val triggerKeyMapFromOtherAppsController = TriggerKeyMapFromOtherAppsController(
        coroutineScope,
        detectKeyMapsUseCase,
        performActionsUseCase,
        detectConstraintsUseCase
    )

    private val fingerprintMapController = FingerprintGestureMapController(
        coroutineScope,
        detectFingerprintMapsUseCase,
        performActionsUseCase,
        detectConstraintsUseCase
    )

    private val keymapDetectionDelegate = KeyMapController(
        coroutineScope,
        detectKeyMapsUseCase,
        performActionsUseCase,
        detectConstraintsUseCase
    )

    private val rerouteKeyEventsController = RerouteKeyEventsController(
        coroutineScope,
        rerouteKeyEventsUseCase
    )

    private var recordingTriggerJob: Job? = null
    private val recordingTrigger: Boolean
        get() = recordingTriggerJob != null && recordingTriggerJob?.isActive == true

    private val isPaused: StateFlow<Boolean> = pauseMappingsUseCase.isPaused
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    private val screenOffTriggersEnabled: StateFlow<Boolean> =
        detectKeyMapsUseCase.detectScreenOffTriggers
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    private val detectScreenOffKeyEventsController =
        DetectScreenOffKeyEventsController(
            suAdapter,
            devicesAdapter
        ) { keyCode, action, device ->

            if (!isPaused.value) {
                withContext(Dispatchers.Main.immediate) {
                    keymapDetectionDelegate.onKeyEvent(
                        keyCode,
                        action,
                        metaState = 0,
                        device = device
                    )
                }
            }
        }

    private var recordingUiElementsJob: Job? = null
    private val recordingUiElements: Boolean
        get() = recordingUiElementsJob != null && recordingUiElementsJob?.isActive == true

    private val initialServiceFlags: Int by lazy {
        var flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            .withFlag(AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS)
            .withFlag(AccessibilityServiceInfo.DEFAULT)
            .withFlag(AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS)
            .withFlag(AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS)
            .withFlag(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = flags.withFlag(AccessibilityServiceInfo.FLAG_ENABLE_ACCESSIBILITY_VOLUME)
        }

        return@lazy flags
    }

    private val initialFeedbackFlags: Int by lazy {

        return@lazy AccessibilityServiceInfo.FEEDBACK_ALL_MASK
    }

    private val initialEventTypes: Int by lazy {

        return@lazy AccessibilityEvent.TYPE_WINDOWS_CHANGED.withFlag(AccessibilityEvent.TYPES_ALL_MASK)
    }

    /*
       On some devices the onServiceConnected method is called multiple times throughout the lifecycle of the service.
       The service flags that the controller *expects* will be stored here. Whenever onServiceConnected is called the
       service's flags will to be updated to these. Whenever these change the controller will check if the service is
       bound and then update them in the service.
        */
    private var serviceFlags: MutableStateFlow<Int> = MutableStateFlow(initialServiceFlags)

    private var serviceFeedbackType: MutableStateFlow<Int> = MutableStateFlow(initialFeedbackFlags)
    private var serviceEventTypes: MutableStateFlow<Int> = MutableStateFlow(initialEventTypes)

    init {
        serviceFlags.onEach { flags ->
            //check that it isn't null because this can only be called once the service is bound
            if (accessibilityService.serviceFlags != null) {
                accessibilityService.serviceFlags = flags
            }
        }.launchIn(coroutineScope)

        serviceFeedbackType.onEach { feedbackType ->
            //check that it isn't null because this can only be called once the service is bound
            if (accessibilityService.serviceFeedbackType != null) {
                accessibilityService.serviceFeedbackType = feedbackType
            }
        }.launchIn(coroutineScope)

        serviceEventTypes.onEach { feedbackType ->
            //check that it isn't null because this can only be called once the service is bound
            if (accessibilityService.serviceEventTypes != null) {
                accessibilityService.serviceEventTypes = feedbackType
            }
        }.launchIn(coroutineScope)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            combine(
                detectFingerprintMapsUseCase.fingerprintMaps,
                isPaused
            ) { fingerprintMaps, isPaused ->
                if (fingerprintMaps.toList()
                        .any { it.isEnabled && it.actionList.isNotEmpty() } && !isPaused
                ) {
                    requestFingerprintGestureDetection()
                } else {
                    denyFingerprintGestureDetection()
                }

            }.launchIn(coroutineScope)
        }

        pauseMappingsUseCase.isPaused.distinctUntilChanged().onEach {
            keymapDetectionDelegate.reset()
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
            .drop(1) //Don't send it when collecting initially
            .onEach { isHidden ->
                if (isHidden) {
                    outputEvents.emit(Event.OnHideKeyboardEvent)
                } else {
                    outputEvents.emit(Event.OnShowKeyboardEvent)
                }
            }.launchIn(coroutineScope)

        combine(
            pauseMappingsUseCase.isPaused,
            merge(detectKeyMapsUseCase.allKeyMapList, detectFingerprintMapsUseCase.fingerprintMaps)
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
                serviceFeedbackType.value = serviceFeedbackType.value
                    .withFlag(AccessibilityServiceInfo.FEEDBACK_GENERIC)

                serviceEventTypes.value = serviceEventTypes.value
                    .withFlag(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    .withFlag(AccessibilityEvent.TYPE_WINDOWS_CHANGED)
                    .withFlag(AccessibilityEvent.TYPE_VIEW_CLICKED)
            } else {
                serviceFeedbackType.value = serviceFeedbackType.value
                    .minusFlag(AccessibilityServiceInfo.FEEDBACK_GENERIC)

                serviceEventTypes.value = serviceEventTypes.value
                    .minusFlag(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    .minusFlag(AccessibilityEvent.TYPE_WINDOWS_CHANGED)
                    .minusFlag(AccessibilityEvent.TYPE_VIEW_CLICKED)
            }
        }.launchIn(coroutineScope)
    }

    fun onServiceConnected() {
        accessibilityService.serviceFlags = serviceFlags.value
        accessibilityService.serviceFeedbackType = serviceFeedbackType.value
        accessibilityService.serviceEventTypes = serviceEventTypes.value

        //check if fingerprint gestures are supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isFingerprintGestureRequested =
                serviceFlags.value.hasFlag(AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES)
            requestFingerprintGestureDetection()

            /* Don't update whether fingerprint gesture detection is supported if it has
            * been supported at some point. Just in case the fingerprint reader is being
            * used while this is called. */
            if (detectFingerprintMapsUseCase.isSupported.firstBlocking() != true) {
                detectFingerprintMapsUseCase.setSupported(
                    accessibilityService.isFingerprintGestureDetectionAvailable
                )
            }

            if (!isFingerprintGestureRequested) {
                denyFingerprintGestureDetection()
            }
        }
    }

    fun onKeyEvent(
        keyCode: Int,
        action: Int,
        device: InputDeviceInfo?,
        metaState: Int,
        scanCode: Int = 0,
        eventTime: Long
    ): Boolean {
        val detailedLogInfo =
            "key code: $keyCode, time since event: ${SystemClock.uptimeMillis() - eventTime}ms, device name: ${device?.name}, descriptor: ${device?.descriptor}, device id: ${device?.id}, is external: ${device?.isExternal}, meta state: $metaState, scan code: $scanCode"

        when (action) {
            KeyEvent.ACTION_DOWN -> Timber.d("Down ${KeyEvent.keyCodeToString(keyCode)}, $detailedLogInfo")
            KeyEvent.ACTION_UP -> Timber.d("Up ${KeyEvent.keyCodeToString(keyCode)}, $detailedLogInfo")
        }

        if (recordingTrigger) {
            if (action == KeyEvent.ACTION_DOWN) {
                Timber.d("Recorded key ${KeyEvent.keyCodeToString(keyCode)}, $detailedLogInfo")
                coroutineScope.launch {
                    outputEvents.emit(
                        Event.RecordedTriggerKey(
                            keyCode,
                            device
                        )
                    )
                }
            }

            return true
        }

        if (!isPaused.value) {
            try {
                var consume: Boolean

                consume = keymapDetectionDelegate.onKeyEvent(
                    keyCode,
                    action,
                    metaState,
                    scanCode,
                    device
                )

                if (!consume) {
                    consume = rerouteKeyEventsController.onKeyEvent(
                        keyCode,
                        action,
                        metaState,
                        scanCode,
                        device
                    )
                }

                when (action) {
                    KeyEvent.ACTION_DOWN -> Timber.d("Down ${KeyEvent.keyCodeToString(keyCode)} - consumed: $consume, $detailedLogInfo")
                    KeyEvent.ACTION_UP -> Timber.d("Up ${KeyEvent.keyCodeToString(keyCode)} - consumed: $consume, $detailedLogInfo")
                }

                return consume

            } catch (e: Exception) {
                Timber.e(e)
            }
        } else {
            when (action) {
                KeyEvent.ACTION_DOWN -> Timber.d("Down ${KeyEvent.keyCodeToString(keyCode)} - not filtering because paused, $detailedLogInfo")
                KeyEvent.ACTION_UP -> Timber.d("Up ${KeyEvent.keyCodeToString(keyCode)} - not filtering because paused, $detailedLogInfo")
            }
        }

        return false
    }

    fun onKeyEventFromIme(
        keyCode: Int,
        action: Int,
        device: InputDeviceInfo?,
        metaState: Int,
        scanCode: Int = 0,
        eventTime: Long
    ): Boolean {
        if (!detectKeyMapsUseCase.acceptKeyEventsFromIme) {
            Timber.d("Don't input key event from ime")
            return false
        }

        /*
        Issue #850
        If a volume key is sent while the phone is ringing or in a call
        then that key event must have been relayed by an input method and only an up event
        is sent. This is a restriction in Android. So send a fake down key event as well.
         */
        if (action == KeyEvent.ACTION_UP && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            onKeyEvent(
                keyCode,
                KeyEvent.ACTION_DOWN,
                device,
                metaState,
                0,
                eventTime
            )
        }

        return onKeyEvent(
            keyCode,
            action,
            device,
            metaState,
            scanCode,
            eventTime
        )
    }

    fun onAccessibilityEvent(event: AccessibilityEventModel?, originalEvent: AccessibilityEvent?) {
        Timber.d("OnAccessibilityEvent $event")

        // TODO: DECREASE EVENTS!!!

        /**
         * Record UI elements and store them into the DB
         */
        if (recordingUiElements && originalEvent != null && RECORD_UI_ELEMENTS_EVENT_TYPES.contains(originalEvent.eventType)) {
            val foundViewIds = accessibilityService.fetchAvailableUIElements()

            if (foundViewIds.isNotEmpty()) {
                foundViewIds.forEachIndexed { index, item ->
                    val splittedViewInfo = item.split("/")

                    if (splittedViewInfo.size == 2) {
                        val elementId = splittedViewInfo[1]
                        val packageName = splittedViewInfo[0]

                        if (packageName != "${BuildConfig.APPLICATION_ID}:id") {
                            viewIdRepository.insert(
                                ViewIdEntity(
                                    id = 0,
                                    viewId = elementId,
                                    packageName = packageName,
                                    fullName = item
                                )
                            )
                        }
                    }
                }
            }

        }


        val focussedNode = accessibilityService.findFocussedNode(AccessibilityNodeInfo.FOCUS_INPUT)

        if (focussedNode?.isEditable == true && focussedNode.isFocused) {
            Timber.d("Got input focus")
            coroutineScope.launch {
                outputEvents.emit(Event.OnInputFocusChange(isFocussed = true))
            }
        } else {
            Timber.d("Lost input focus")
            coroutineScope.launch {
                outputEvents.emit(Event.OnInputFocusChange(isFocussed = false))
            }
        }
    }

    fun onFingerprintGesture(id: FingerprintMapId) {
        fingerprintMapController.onGesture(id)
    }

    fun triggerKeyMapFromIntent(uid: String) {
        triggerKeyMapFromOtherAppsController.onDetected(uid)
    }

    @SuppressLint("NewApi")
    private suspend fun onEventFromUi(event: Event) {
        Timber.d("Service received event from UI: $event")
        when (event) {
            is Event.StartRecordingTrigger ->
                if (!recordingTrigger) {
                    recordingTriggerJob = recordTriggerJob()
                }

            is Event.StopRecordingTrigger -> {
                val wasRecordingTrigger = recordingTrigger

                recordingTriggerJob?.cancel()
                recordingTriggerJob = null

                if (wasRecordingTrigger) {
                    coroutineScope.launch {
                        outputEvents.emit(Event.OnStoppedRecordingTrigger)
                    }
                }
            }

            is Event.TestAction -> performActionsUseCase.perform(event.action)
            is Event.Ping -> coroutineScope.launch { outputEvents.emit(Event.Pong(event.key)) }
            is Event.HideKeyboard -> accessibilityService.hideKeyboard()
            is Event.ShowKeyboard -> accessibilityService.showKeyboard()
            is Event.ChangeIme -> accessibilityService.switchIme(event.imeId)
            is Event.DisableService -> accessibilityService.disableSelf()
            is Event.StartRecordingUiElements ->
                if (!recordingUiElements) {
                    recordingUiElementsJob = recordUiElementsJob()
                }
            is Event.StopRecordingUiElements -> {
                val wasRecordingUiElements = recordingUiElements

                recordingUiElementsJob?.cancel()
                recordingUiElementsJob = null

                if (wasRecordingUiElements) {
                    coroutineScope.launch {
                        outputEvents.emit(Event.OnStoppedRecordingUiElements)
                    }
                }
            }
            is Event.ClearRecordedUiElements -> coroutineScope.launch {
                viewIdRepository.deleteAll()
            }
            else -> Unit
        }
    }

    private fun recordTriggerJob() = coroutineScope.launch {
        repeat(RECORD_TRIGGER_TIMER_LENGTH) { iteration ->
            if (isActive) {
                val timeLeft = RECORD_TRIGGER_TIMER_LENGTH - iteration
                outputEvents.emit(Event.OnIncrementRecordTriggerTimer(timeLeft))

                delay(1000)
            }
        }

        outputEvents.emit(Event.OnStoppedRecordingTrigger)
    }

    private fun recordUiElementsJob() = coroutineScope.launch {
        repeat(RECORD_UI_ELEMENTS_TIMER_LENGTH) {iteration ->
            if (isActive) {
                val timeLeft = RECORD_UI_ELEMENTS_TIMER_LENGTH - iteration
                outputEvents.emit(Event.OnIncrementRecordUiElementsTimer(timeLeft))

                delay(1000)
            }
        }

        outputEvents.emit(Event.OnStoppedRecordingUiElements)
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