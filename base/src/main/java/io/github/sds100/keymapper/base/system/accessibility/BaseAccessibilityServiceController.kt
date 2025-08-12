package io.github.sds100.keymapper.base.system.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.lifecycleScope
import io.github.sds100.keymapper.api.IKeyEventRelayServiceCallback
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.actions.PerformActionsUseCaseImpl
import io.github.sds100.keymapper.base.actions.TestActionEvent
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCaseImpl
import io.github.sds100.keymapper.base.detection.DetectKeyMapsUseCaseImpl
import io.github.sds100.keymapper.base.detection.KeyMapDetectionController
import io.github.sds100.keymapper.base.detection.TriggerKeyMapFromOtherAppsController
import io.github.sds100.keymapper.base.input.InputEventDetectionSource
import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.TriggerKeyMapEvent
import io.github.sds100.keymapper.base.reroutekeyevents.RerouteKeyEventsController
import io.github.sds100.keymapper.base.trigger.RecordTriggerController
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.common.utils.hasFlag
import io.github.sds100.keymapper.common.utils.minusFlag
import io.github.sds100.keymapper.common.utils.withFlag
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent
import io.github.sds100.keymapper.system.inputevents.KMGamePadEvent
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
import io.github.sds100.keymapper.system.inputmethod.KeyEventRelayServiceWrapper
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
import kotlinx.coroutines.launch
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
    private val settingsRepository: PreferenceRepository,
    private val keyEventRelayServiceWrapper: KeyEventRelayServiceWrapper,
    private val inputEventHub: InputEventHub,
    private val recordTriggerController: RecordTriggerController,
    private val setupAssistantControllerFactory: SystemBridgeSetupAssistantController.Factory
) {
    companion object {
        private const val DEFAULT_NOTIFICATION_TIMEOUT = 200L
        private const val CALLBACK_ID_ACCESSIBILITY_SERVICE = "accessibility_service"
    }

    private val performActionsUseCase = performActionsUseCaseFactory.create(
        accessibilityService = service,
    )

    private val detectKeyMapsUseCase = detectKeyMapsUseCaseFactory.create(
        accessibilityService = service,
        coroutineScope = service.lifecycleScope,
    )

    val detectConstraintsUseCase = detectConstraintsUseCaseFactory.create(service)

    val keyMapDetectionController = KeyMapDetectionController(
        service.lifecycleScope,
        detectKeyMapsUseCase,
        performActionsUseCase,
        detectConstraintsUseCase,
        inputEventHub,
        pauseKeyMapsUseCase,
        recordTriggerController,
    )

    val triggerKeyMapFromOtherAppsController = TriggerKeyMapFromOtherAppsController(
        service.lifecycleScope,
        detectKeyMapsUseCase,
        performActionsUseCase,
        detectConstraintsUseCase,
    )

    val rerouteKeyEventsController: RerouteKeyEventsController =
        rerouteKeyEventsControllerFactory.create(
            service.lifecycleScope,
        )

    val accessibilityNodeRecorder = accessibilityNodeRecorderFactory.create(service)

    private val setupAssistantController: SystemBridgeSetupAssistantController? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setupAssistantControllerFactory.create(service.lifecycleScope, service)
        } else {
            null
        }

    val isPaused: StateFlow<Boolean> =
        pauseKeyMapsUseCase.isPaused
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

    private val relayServiceCallback: IKeyEventRelayServiceCallback =
        object : IKeyEventRelayServiceCallback.Stub() {
            override fun onKeyEvent(event: KeyEvent?): Boolean {
                event ?: return false

                val kmKeyEvent = KMKeyEvent.fromAndroidKeyEvent(event) ?: return false
                return onKeyEventFromIme(kmKeyEvent)
            }

            override fun onMotionEvent(event: MotionEvent?): Boolean {
                event ?: return false

                val gamePadEvent = KMGamePadEvent.fromMotionEvent(event)
                    ?: return false
                return onMotionEventFromIme(gamePadEvent)
            }
        }

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
            triggerKeyMapFromOtherAppsController.reset()
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

        keyEventRelayServiceWrapper.registerClient(
            CALLBACK_ID_ACCESSIBILITY_SERVICE,
            relayServiceCallback,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setupAssistantController?.onServiceConnected()
        }
    }

    open fun onDestroy() {
        keyMapDetectionController.teardown()
        keyEventRelayServiceWrapper.unregisterClient(CALLBACK_ID_ACCESSIBILITY_SERVICE)
        accessibilityNodeRecorder.teardown()
        rerouteKeyEventsController.teardown()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setupAssistantController?.teardown()
        }
    }

    open fun onConfigurationChanged(newConfig: Configuration) {
    }

    fun onKeyEvent(
        event: KMKeyEvent,
        detectionSource: InputEventDetectionSource = InputEventDetectionSource.ACCESSIBILITY_SERVICE,
    ): Boolean {
        return inputEventHub.onInputEvent(event, detectionSource)
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
            inputEventHub.onInputEvent(
                event.copy(action = KeyEvent.ACTION_DOWN),
                detectionSource = InputEventDetectionSource.INPUT_METHOD,
            )
        }

        return inputEventHub.onInputEvent(event, InputEventDetectionSource.INPUT_METHOD)
    }

    fun onMotionEventFromIme(event: KMGamePadEvent): Boolean {
        return inputEventHub.onInputEvent(
            event,
            detectionSource = InputEventDetectionSource.INPUT_METHOD,
        )
    }

    open fun onAccessibilityEvent(event: AccessibilityEvent) {
        accessibilityNodeRecorder.onAccessibilityEvent(event)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setupAssistantController?.onAccessibilityEvent(event)
        }

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
    }

    fun onFingerprintGesture(type: FingerprintGestureType) {
        keyMapDetectionController.onFingerprintGesture(type)
    }

    private fun triggerKeyMapFromIntent(uid: String) {
        triggerKeyMapFromOtherAppsController.onDetected(uid)
    }

    open fun onEventFromUi(event: AccessibilityServiceEvent) {
        Timber.d("Service received event from UI: $event")

        when (event) {
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
