package io.github.sds100.keymapper.base.system.accessibility

import android.content.res.Configuration
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import io.github.sds100.keymapper.api.IKeyEventRelayServiceCallback
import io.github.sds100.keymapper.base.actions.PerformActionsUseCaseImpl
import io.github.sds100.keymapper.base.actions.TestActionEvent
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCaseImpl
import io.github.sds100.keymapper.base.detection.DetectKeyMapsUseCaseImpl
import io.github.sds100.keymapper.base.detection.KeyMapDetectionController
import io.github.sds100.keymapper.base.detection.TriggerKeyMapFromOtherAppsController
import io.github.sds100.keymapper.base.expertmode.SystemBridgeSetupAssistantController
import io.github.sds100.keymapper.base.input.InputEventDetectionSource
import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.TriggerKeyMapEvent
import io.github.sds100.keymapper.base.system.inputmethod.AutoSwitchImeController
import io.github.sds100.keymapper.base.trigger.RecordTriggerController
import io.github.sds100.keymapper.base.trigger.RecordTriggerState
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent
import io.github.sds100.keymapper.system.inputevents.KMGamePadEvent
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
import io.github.sds100.keymapper.system.inputmethod.KeyEventRelayServiceWrapper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

abstract class BaseAccessibilityServiceController(
    private val service: BaseAccessibilityService,
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
    private val setupAssistantControllerFactory: SystemBridgeSetupAssistantController.Factory,
    private val autoSwitchImeControllerFactory: AutoSwitchImeController.Factory,
) {
    companion object {
        private const val CALLBACK_ID_ACCESSIBILITY_SERVICE = "accessibility_service"

        private const val FEATURE_SOURCE_KEY_MAPS = "key_maps"
        private const val FEATURE_SOURCE_RECORD_TRIGGER = "record_trigger"
        private const val FEATURE_SOURCE_RECORD_NODES = "record_nodes"
        private const val FEATURE_SOURCE_CHANGE_IME_ON_INPUT_FOCUS = "change_ime_on_input_focus"
        private const val FEATURE_SOURCE_SETUP_ASSISTANT = "setup_assistant"
    }

    private val performActionsUseCase = performActionsUseCaseFactory.create(
        accessibilityService = service,
        coroutineScope = service.lifecycleScope,
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
        settingsRepository,
    )

    val triggerKeyMapFromOtherAppsController = TriggerKeyMapFromOtherAppsController(
        service.lifecycleScope,
        detectKeyMapsUseCase,
        performActionsUseCase,
        detectConstraintsUseCase,
    )

    val accessibilityNodeRecorder = accessibilityNodeRecorderFactory.create(service)

    private val setupAssistantController: SystemBridgeSetupAssistantController =
        setupAssistantControllerFactory.create(service.lifecycleScope, service)

    private val autoSwitchImeController: AutoSwitchImeController? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            autoSwitchImeControllerFactory.create(service, service.lifecycleScope)
        } else {
            null
        }
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

    /**
     * Only requests the accessibility service features that are needed for the features the user
     * is actually using. See [AccessibilityServiceFeature].
     */
    val serviceInfoController: AccessibilityServiceInfoController =
        AccessibilityServiceInfoController(service, service.lifecycleScope)

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
        registerFeatureSources()

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

        service.lifecycleScope.launch {
            accessibilityNodeRecorder.recordState.collectLatest { state ->
                outputEvents.emit(RecordAccessibilityNodeEvent.OnRecordNodeStateChanged(state))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            autoSwitchImeController?.init()
        }
    }

    /**
     * Register everything that needs accessibility service features so that only the flags and
     * event types that are needed right now are requested. See [AccessibilityServiceFeature].
     */
    private fun registerFeatureSources() {
        serviceInfoController.setFeatureSource(
            FEATURE_SOURCE_KEY_MAPS,
            combine(
                detectKeyMapsUseCase.allKeyMapList,
                isPaused,
            ) { keyMaps, isPaused ->
                // No input events are handled while the key maps are paused so nothing needs
                // to be requested for them.
                if (isPaused) {
                    emptySet()
                } else {
                    AccessibilityServiceFeatureUtils.getRequiredFeatures(keyMaps)
                }
            },
        )

        serviceInfoController.setFeatureSource(
            FEATURE_SOURCE_RECORD_TRIGGER,
            recordTriggerController.state.map { state ->
                if (state is RecordTriggerState.CountingDown) {
                    setOf(AccessibilityServiceFeature.FILTER_KEY_EVENTS)
                } else {
                    emptySet()
                }
            },
        )

        serviceInfoController.setFeatureSource(
            FEATURE_SOURCE_RECORD_NODES,
            accessibilityNodeRecorder.recordState.map { state ->
                if (state is RecordAccessibilityNodeState.CountingDown) {
                    setOf(
                        AccessibilityServiceFeature.WINDOW_STATE,
                        AccessibilityServiceFeature.NODE_INFO,
                        AccessibilityServiceFeature.VIEW_INTERACTION,
                        // The nodes must be recorded as soon as the user interacts with them.
                        AccessibilityServiceFeature.IMMEDIATE_EVENTS,
                    )
                } else {
                    emptySet()
                }
            },
        )

        serviceInfoController.setFeatureSource(
            FEATURE_SOURCE_CHANGE_IME_ON_INPUT_FOCUS,
            changeImeOnInputFocusFlow.map { changeImeOnInputFocus ->
                if (!changeImeOnInputFocus) {
                    return@map emptySet()
                }

                buildSet {
                    add(AccessibilityServiceFeature.VIEW_INTERACTION)

                    // Android 13+ uses the accessibility service's own input method to detect
                    // when input starts. Older versions watch for window changes instead.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(AccessibilityServiceFeature.INPUT_METHOD_EDITOR)
                    } else {
                        add(AccessibilityServiceFeature.WINDOW_STATE)
                    }
                }
            },
        )

        serviceInfoController.setFeatureSource(
            FEATURE_SOURCE_SETUP_ASSISTANT,
            setupAssistantController.isInteractive.map { isInteractive ->
                if (isInteractive) {
                    setOf(
                        AccessibilityServiceFeature.WINDOW_STATE,
                        AccessibilityServiceFeature.NODE_INFO,
                    )
                } else {
                    emptySet()
                }
            },
        )
    }

    open fun onServiceConnected() {
        serviceInfoController.onServiceConnected()

        /* Don't update whether fingerprint gesture detection is supported if it has
         * been supported at some point. Just in case the fingerprint reader is being
         * used while this is called. */
        if (fingerprintGesturesSupported.isSupported.firstBlocking() != true) {
            // Fingerprint gesture detection is only available while the service is requesting
            // it, so request it for as long as it takes to check.
            val isAvailable = runBlocking {
                serviceInfoController.withFeature(
                    setOf(AccessibilityServiceFeature.FINGERPRINT_GESTURES),
                ) {
                    service.isFingerprintGestureDetectionAvailable
                }
            }

            fingerprintGesturesSupported.setSupported(isAvailable)
        }

        keyEventRelayServiceWrapper.registerClient(
            CALLBACK_ID_ACCESSIBILITY_SERVICE,
            relayServiceCallback,
        )

        setupAssistantController.onServiceConnected()
    }

    open fun onDestroy() {
        keyMapDetectionController.teardown()
        keyEventRelayServiceWrapper.unregisterClient(CALLBACK_ID_ACCESSIBILITY_SERVICE)
        accessibilityNodeRecorder.teardown()

        setupAssistantController.teardown()
    }

    open fun onConfigurationChanged(newConfig: Configuration) {
    }

    fun onKeyEvent(
        event: KMKeyEvent,
        detectionSource: InputEventDetectionSource =
            InputEventDetectionSource.ACCESSIBILITY_SERVICE,
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
        if (event.action == KeyEvent.ACTION_UP &&
            (
                event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                    event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                )
        ) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            autoSwitchImeController?.onAccessibilityEvent(event)
        }

        setupAssistantController.onAccessibilityEvent(event)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        autoSwitchImeController?.onStartInput(attribute, restarting)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun onFinishInput() {
        autoSwitchImeController?.onFinishInput()
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
                val actionFeatures =
                    AccessibilityServiceFeatureUtils.getRequiredFeatures(event.action)

                serviceInfoController.withFeature(actionFeatures) {
                    performActionsUseCase.perform(event.action)
                }
            }

            is AccessibilityServiceEvent.Ping -> service.lifecycleScope.launch {
                outputEvents.emit(AccessibilityServiceEvent.Pong(event.key))
            }

            is AccessibilityServiceEvent.HideKeyboard -> service.hideKeyboard()

            is AccessibilityServiceEvent.ShowKeyboard -> service.showKeyboard()

            is AccessibilityServiceEvent.ChangeIme ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    service.switchIme(event.imeId)
                }

            is AccessibilityServiceEvent.DisableService ->
                service.disableSelf()

            is TriggerKeyMapEvent -> triggerKeyMapFromIntent(event.uid)

            is AccessibilityServiceEvent.EnableInputMethod -> if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {
                service.enableIme(event.imeId)
            }

            is RecordAccessibilityNodeEvent.StartRecordingNodes -> {
                accessibilityNodeRecorder.startRecording()
            }

            is RecordAccessibilityNodeEvent.StopRecordingNodes -> {
                accessibilityNodeRecorder.stopRecording()
            }

            is AccessibilityServiceEvent.GlobalAction -> {
                service.doGlobalAction(event.action)
            }

            is AccessibilityServiceEvent.OnKeyMapperImeStartInput -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    autoSwitchImeController?.onStartInput(event.attribute, event.restarting)
                }
            }

            else -> Unit
        }
    }
}
