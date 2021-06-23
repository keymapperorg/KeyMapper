package io.github.sds100.keymapper.system.accessibility

import android.os.Build
import android.view.KeyEvent
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.actions.SoundAction
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
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
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * Created by sds100 on 17/04/2021.
 */
class AccessibilityServiceController(
    val coroutineScope: CoroutineScope,
    val accessibilityService: IAccessibilityService,
    val inputEvents: SharedFlow<Event>,
    val outputEvents: MutableSharedFlow<Event>,
    val detectConstraintsUseCase: DetectConstraintsUseCase,
    val performActionsUseCase: PerformActionsUseCase,
    val detectKeyMapsUseCase: DetectKeyMapsUseCase,
    val detectFingerprintMapsUseCase: DetectFingerprintMapsUseCase,
    rerouteKeyEventsUseCase: RerouteKeyEventsUseCase,
    val pauseMappingsUseCase: PauseMappingsUseCase,
    val devicesAdapter: DevicesAdapter,
    val suAdapter: SuAdapter
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
        ) { keyCode, action, deviceDescriptor, isExternal, deviceId ->

            if (!isPaused.value) {
                withContext(Dispatchers.Main.immediate) {
                    keymapDetectionDelegate.onKeyEvent(
                        keyCode,
                        action,
                        deviceDescriptor,
                        isExternal,
                        0,
                        deviceId
                    )
                }
            }
        }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            checkFingerprintGesturesAvailability()

            combine(
                detectFingerprintMapsUseCase.fingerprintMaps,
                isPaused
            ) { fingerprintMaps, isPaused ->
                if (fingerprintMaps.toList()
                        .any { it.isEnabled && it.actionList.isNotEmpty() } && !isPaused
                ) {
                    accessibilityService.requestFingerprintGestureDetection()
                } else {
                    accessibilityService.denyFingerprintGestureDetection()
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

        accessibilityService.isKeyboardHidden.onEach { isHidden ->
            if (isHidden) {
                outputEvents.emit(OnHideKeyboardEvent)
            } else {
                outputEvents.emit(OnShowKeyboardEvent)
            }
        }.launchIn(coroutineScope)

        combine(
            pauseMappingsUseCase.isPaused,
            merge(detectKeyMapsUseCase.allKeyMapList, detectFingerprintMapsUseCase.fingerprintMaps)
        ) { isPaused, mappings ->
            var enableAccessibilityVolumeStream: Boolean

            if (isPaused) {
                enableAccessibilityVolumeStream = false
            } else {
                enableAccessibilityVolumeStream = mappings.any { mapping ->
                    mapping.isEnabled && mapping.actionList.any { it.data is SoundAction }
                }
            }

            if (enableAccessibilityVolumeStream) {
                accessibilityService.enableAccessibilityVolumeStream()
            } else {
                accessibilityService.disableAccessibilityVolumeStream()
            }
        }.launchIn(coroutineScope)
    }

    fun onKeyEvent(
        keyCode: Int,
        action: Int,
        deviceName: String,
        descriptor: String,
        isExternal: Boolean,
        metaState: Int,
        deviceId: Int,
        scanCode: Int = 0
    ): Boolean {
        val detailedLogInfo =
            "key code: $keyCode, device name: $deviceName, descriptor: $descriptor, device id: $deviceId, is external: $isExternal, meta state: $metaState, scan code: $scanCode"

        when (action) {
            KeyEvent.ACTION_DOWN -> Timber.d("Down ${KeyEvent.keyCodeToString(keyCode)}, $detailedLogInfo")
            KeyEvent.ACTION_UP -> Timber.d("Up ${KeyEvent.keyCodeToString(keyCode)}, $detailedLogInfo")
        }

        if (recordingTrigger) {
            if (action == KeyEvent.ACTION_DOWN) {
                Timber.d("Recorded key ${KeyEvent.keyCodeToString(keyCode)}, $detailedLogInfo")
                coroutineScope.launch {
                    outputEvents.emit(
                        RecordedTriggerKeyEvent(
                            keyCode,
                            deviceName,
                            descriptor,
                            isExternal
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
                    descriptor,
                    isExternal,
                    metaState,
                    deviceId,
                    scanCode
                )

                if (!consume) {
                    consume = rerouteKeyEventsController.onKeyEvent(
                        keyCode,
                        action,
                        descriptor,
                        isExternal,
                        metaState,
                        deviceId,
                        scanCode
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

    fun onFingerprintGesture(id: FingerprintMapId) {
        fingerprintMapController.onGesture(id)
    }

    fun triggerKeyMapFromIntent(uid: String) {
        triggerKeyMapFromOtherAppsController.onDetected(uid)
    }

    private fun onEventFromUi(event: Event) {
        when (event) {
            is StartRecordingTrigger ->
                if (!recordingTrigger) {
                    recordingTriggerJob = recordTriggerJob()
                }

            is StopRecordingTrigger -> {
                val wasRecordingTrigger = recordingTrigger

                recordingTriggerJob?.cancel()
                recordingTriggerJob = null

                if (wasRecordingTrigger) {
                    coroutineScope.launch {
                        outputEvents.emit(OnStoppedRecordingTrigger)
                    }
                }
            }

            is TestActionEvent -> performActionsUseCase.perform(event.action)

            is Ping -> coroutineScope.launch { outputEvents.emit(Pong(event.key)) }
            is HideKeyboardEvent -> accessibilityService.hideKeyboard()
            is ShowKeyboardEvent -> accessibilityService.showKeyboard()
            is ChangeIme -> accessibilityService.switchIme(event.imeId)
        }
    }

    private fun checkFingerprintGesturesAvailability() {
        accessibilityService.requestFingerprintGestureDetection()

        //this is important
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /* Don't update whether fingerprint gesture detection is supported if it has
            * been supported at some point. Just in case the fingerprint reader is being
            * used while this is called. */
            if (detectFingerprintMapsUseCase.isSupported.firstBlocking() != true) {
                detectFingerprintMapsUseCase.setSupported(
                    accessibilityService.isGestureDetectionAvailable
                )
            }
        }

        accessibilityService.denyFingerprintGestureDetection()
    }

    private fun recordTriggerJob() = coroutineScope.launch {
        repeat(RECORD_TRIGGER_TIMER_LENGTH) { iteration ->
            if (isActive) {
                val timeLeft = RECORD_TRIGGER_TIMER_LENGTH - iteration
                outputEvents.emit(OnIncrementRecordTriggerTimer(timeLeft))

                delay(1000)
            }
        }

        outputEvents.emit(OnStoppedRecordingTrigger)
    }
}