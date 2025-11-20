package io.github.sds100.keymapper.base.trigger

import android.view.KeyEvent
import io.github.sds100.keymapper.base.detection.DpadMotionEventTracker
import io.github.sds100.keymapper.base.input.InputEventDetectionSource
import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.input.InputEventHubCallback
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.isError
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent
import io.github.sds100.keymapper.system.inputevents.KMEvdevEvent
import io.github.sds100.keymapper.system.inputevents.KMGamePadEvent
import io.github.sds100.keymapper.system.inputevents.KMInputEvent
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
import io.github.sds100.keymapper.system.inputevents.Scancode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

@Singleton
class RecordTriggerControllerImpl @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val inputEventHub: InputEventHub,
    private val accessibilityServiceAdapter: AccessibilityServiceAdapter,
) : RecordTriggerController,
    InputEventHubCallback {
    companion object {
        /**
         * How long should the accessibility service record a trigger in seconds.
         */
        private const val RECORD_TRIGGER_TIMER_LENGTH = 5
        private const val INPUT_EVENT_HUB_ID = "record_trigger"

        private val SCAN_CODES_BLACKLIST = setOf(
            Scancode.BTN_TOUCH,
            Scancode.BTN_TOOL_FINGER,
        )
    }

    override val state = MutableStateFlow<RecordTriggerState>(RecordTriggerState.Idle)

    private var recordingTriggerJob: Job? = null

    private val recordingTrigger: Boolean
        get() = recordingTriggerJob != null && recordingTriggerJob?.isActive == true

    private val recordedKeys: MutableList<RecordedKey> = mutableListOf()
    override val onRecordKey: MutableSharedFlow<RecordedKey> = MutableSharedFlow()

    /**
     * The keys that are currently being held down. They are removed from the set when the up event
     * is received and it is cleared when recording starts.
     */
    private val downKeyEvents: MutableSet<KMKeyEvent> = mutableSetOf()
    private val downEvdevEvents: MutableSet<KMEvdevEvent> = mutableSetOf()
    private val dpadMotionEventTracker: DpadMotionEventTracker = DpadMotionEventTracker()

    override val isEvdevRecordingEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun setEvdevRecordingEnabled(enabled: Boolean) {
        if (state.value is RecordTriggerState.CountingDown) {
            return
        }

        isEvdevRecordingEnabled.value = enabled
    }

    override fun onInputEvent(
        event: KMInputEvent,
        detectionSource: InputEventDetectionSource,
    ): Boolean {
        if (!recordingTrigger) {
            return false
        }

        when (event) {
            is KMEvdevEvent -> {
                if (!isEvdevRecordingEnabled.value) {
                    return false
                }

                // Do not record evdev events that are not key events.
                if (event.type != KMEvdevEvent.TYPE_KEY_EVENT) {
                    return false
                }

                if (SCAN_CODES_BLACKLIST.contains(event.code)) {
                    return false
                }

                // Must also remove old down events if a new down even is received.
                val matchingDownEvent: KMEvdevEvent? = downEvdevEvents.find {
                    it == event.copy(value = KMEvdevEvent.VALUE_DOWN)
                }

                if (matchingDownEvent != null) {
                    downEvdevEvents.remove(matchingDownEvent)
                }

                if (event.isDownEvent) {
                    downEvdevEvents.add(event)
                } else if (event.isUpEvent) {
                    onRecordKey(createEvdevRecordedKey(event))
                    Timber.d(
                        "Recorded evdev event ${event.code} ${KeyEvent.keyCodeToString(
                            event.androidCode,
                        )}",
                    )
                }

                return true
            }

            is KMGamePadEvent -> {
                val dpadKeyEvents = dpadMotionEventTracker.convertMotionEvent(event)

                for (keyEvent in dpadKeyEvents) {
                    if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                        val recordedKey = createKeyEventRecordedKey(
                            keyEvent,
                            detectionSource,
                        )
                        onRecordKey(recordedKey)
                        Timber.d(
                            "Recorded motion event ${KeyEvent.keyCodeToString(keyEvent.keyCode)}",
                        )
                    }
                }
                return true
            }

            is KMKeyEvent -> {
                val matchingDownEvent: KMKeyEvent? = downKeyEvents.find {
                    it.keyCode == event.keyCode &&
                        it.scanCode == event.scanCode &&
                        it.deviceId == event.deviceId &&
                        it.source == event.source
                }

                // Must also remove old down events if a new down even is received.
                if (matchingDownEvent != null) {
                    downKeyEvents.remove(matchingDownEvent)
                }

                if (event.action == KeyEvent.ACTION_DOWN) {
                    downKeyEvents.add(event)
                } else if (event.action == KeyEvent.ACTION_UP) {
                    // Only record the key if there is a matching down event.
                    // Do not do this when recording motion events from the input method
                    // or Activity because they intentionally only input a down event.
                    if (matchingDownEvent != null) {
                        val recordedKey = createKeyEventRecordedKey(event, detectionSource)
                        onRecordKey(recordedKey)
                        Timber.d("Recorded key event ${KeyEvent.keyCodeToString(event.keyCode)}")
                    }
                }
                return true
            }
        }
    }

    override suspend fun startRecording(): KMResult<*> {
        val serviceResult =
            accessibilityServiceAdapter.send(AccessibilityServiceEvent.Ping("record_trigger"))

        if (serviceResult.isError) {
            return serviceResult
        }

        if (recordingTrigger) {
            return Success(Unit)
        }

        recordingTriggerJob = recordTriggerJob()

        return Success(Unit)
    }

    override fun stopRecording(): KMResult<*> {
        recordingTriggerJob?.cancel()
        recordingTriggerJob = null

        dpadMotionEventTracker.reset()
        inputEventHub.unregisterClient(INPUT_EVENT_HUB_ID)
        state.update { RecordTriggerState.Completed(recordedKeys) }

        return Success(Unit)
    }

    private fun onRecordKey(recordedKey: RecordedKey) {
        recordedKeys.add(recordedKey)
        runBlocking { onRecordKey.emit(recordedKey) }
    }

    private fun createKeyEventRecordedKey(
        keyEvent: KMKeyEvent,
        detectionSource: InputEventDetectionSource,
    ): RecordedKey.KeyEvent {
        return RecordedKey.KeyEvent(
            keyCode = keyEvent.keyCode,
            scanCode = keyEvent.scanCode,
            deviceDescriptor = keyEvent.device.descriptor,
            deviceName = keyEvent.device.name,
            isExternalDevice = keyEvent.device.isExternal,
            detectionSource = detectionSource,
        )
    }

    private fun createEvdevRecordedKey(evdevEvent: KMEvdevEvent): RecordedKey.EvdevEvent {
        return RecordedKey.EvdevEvent(
            keyCode = evdevEvent.androidCode,
            scanCode = evdevEvent.code,
            device = evdevEvent.device,
        )
    }

    // Run on a different thread in case the main thread is locked up while recording and
    // the evdev devices aren't ungrabbed.
    private fun recordTriggerJob(): Job = coroutineScope.launch(Dispatchers.Default) {
        val isEvdevRecordingEnabled = isEvdevRecordingEnabled.value

        Timber.i("Starting trigger recording. Evdev recording: $isEvdevRecordingEnabled")

        recordedKeys.clear()
        dpadMotionEventTracker.reset()
        downKeyEvents.clear()

        val evdevEventTypes = if (isEvdevRecordingEnabled) {
            listOf(KMEvdevEvent.TYPE_KEY_EVENT)
        } else {
            emptyList()
        }

        inputEventHub.registerClient(
            INPUT_EVENT_HUB_ID,
            this@RecordTriggerControllerImpl,
            evdevEventTypes,
        )

        if (isEvdevRecordingEnabled) {
            // Grab all evdev devices
            inputEventHub.grabAllEvdevDevices(INPUT_EVENT_HUB_ID)
        }

        repeat(RECORD_TRIGGER_TIMER_LENGTH) { iteration ->
            val timeLeft = RECORD_TRIGGER_TIMER_LENGTH - iteration

            state.update { RecordTriggerState.CountingDown(timeLeft) }

            delay(1000)
        }

        downKeyEvents.clear()
        dpadMotionEventTracker.reset()
        inputEventHub.unregisterClient(INPUT_EVENT_HUB_ID)
        state.update { RecordTriggerState.Completed(recordedKeys) }
    }
}

interface RecordTriggerController {
    val state: StateFlow<RecordTriggerState>
    val onRecordKey: Flow<RecordedKey>

    val isEvdevRecordingEnabled: StateFlow<Boolean>
    fun setEvdevRecordingEnabled(enabled: Boolean)

    /**
     * @return Success if started and an Error if failed to start.
     */
    suspend fun startRecording(): KMResult<*>
    fun stopRecording(): KMResult<*>
}
