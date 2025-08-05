package io.github.sds100.keymapper.base.trigger

import android.view.KeyEvent
import io.github.sds100.keymapper.base.input.InputEventDetectionSource
import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.input.InputEventHubCallback
import io.github.sds100.keymapper.base.keymaps.detection.DpadMotionEventTracker
import io.github.sds100.keymapper.common.utils.InputDeviceInfo
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.common.utils.isError
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.system.inputevents.KMEvdevEvent
import io.github.sds100.keymapper.system.inputevents.KMGamePadEvent
import io.github.sds100.keymapper.system.inputevents.KMInputEvent
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordTriggerControllerImpl @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val inputEventHub: InputEventHub,
    private val accessibilityServiceAdapter: AccessibilityServiceAdapter,
    private val devicesAdapter: DevicesAdapter
) : RecordTriggerController, InputEventHubCallback {
    companion object {
        /**
         * How long should the accessibility service record a trigger in seconds.
         */
        private const val RECORD_TRIGGER_TIMER_LENGTH = 5
        private const val INPUT_EVENT_HUB_ID = "record_trigger"
    }

    override val state = MutableStateFlow<RecordTriggerState>(RecordTriggerState.Idle)

    private var recordingTriggerJob: Job? = null

    private val recordingTrigger: Boolean
        get() = recordingTriggerJob != null && recordingTriggerJob?.isActive == true

    private val recordedKeys: MutableList<RecordedKey> = mutableListOf()
    override val onRecordKey: MutableSharedFlow<RecordedKey> = MutableSharedFlow()

    private val dpadMotionEventTracker: DpadMotionEventTracker = DpadMotionEventTracker()

    override fun onInputEvent(
        event: KMInputEvent,
        detectionSource: InputEventDetectionSource
    ): Boolean {
        if (!recordingTrigger) {
            return false
        }

        when (event) {
            is KMEvdevEvent -> {
                if (!event.isKeyEvent || event.androidCode == null) {
                    return false
                }

                val device = devicesAdapter.getInputDevice(event.deviceId)

                val recordedKey = createRecordedKey(event.androidCode!!, device, detectionSource)

                onRecordKey(recordedKey)
            }

            is KMGamePadEvent -> {
                val dpadKeyEvents = dpadMotionEventTracker.convertMotionEvent(event)

                for (keyEvent in dpadKeyEvents) {
                    if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                        Timber.d("Recorded motion event ${KeyEvent.keyCodeToString(keyEvent.keyCode)}")

                        val recordedKey = createRecordedKey(
                            keyEvent.keyCode,
                            keyEvent.device,
                            detectionSource
                        )
                        onRecordKey(recordedKey)
                    }
                }
            }

            is KMKeyEvent -> {
                val recordedKey = createRecordedKey(event.keyCode, event.device, detectionSource)
                onRecordKey(recordedKey)
            }
        }

        return true
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

        recordedKeys.clear()
        dpadMotionEventTracker.reset()

        recordingTriggerJob = recordTriggerJob()

        inputEventHub.registerClient(INPUT_EVENT_HUB_ID, this)

        val inputDevices = devicesAdapter.connectedInputDevices.value.dataOrNull()

        // Grab all evdev devices
        if (inputDevices != null) {
            val allDeviceDescriptors = inputDevices.map { it.descriptor }.toList()
            inputEventHub.setGrabbedEvdevDevices(INPUT_EVENT_HUB_ID, allDeviceDescriptors)
        }

        return Success(Unit)
    }

    override suspend fun stopRecording(): KMResult<*> {
        recordingTriggerJob?.cancel()
        recordingTriggerJob = null
        dpadMotionEventTracker.reset()
        inputEventHub.unregisterClient(INPUT_EVENT_HUB_ID)

        state.update { RecordTriggerState.Completed(recordedKeys) }

        return Success(Unit)
    }

    /**
     * Process motion events from the activity so that DPAD buttons can be recorded
     * even when the Key Mapper IME is not being used. DO NOT record the key events because
     * these are sent from the joy sticks.
     * @return Whether the motion event is consumed.
     */
    fun onActivityMotionEvent(event: KMGamePadEvent): Boolean {
        if (state.value !is RecordTriggerState.CountingDown) {
            return false
        }

        val keyEvent =
            dpadMotionEventTracker.convertMotionEvent(event).firstOrNull() ?: return false

        if (!InputEventUtils.isDpadKeyCode(keyEvent.keyCode)) {
            return false
        }

        if (keyEvent.action == KeyEvent.ACTION_UP) {
            val recordedKey = createRecordedKey(
                keyEvent.keyCode,
                keyEvent.device,
                InputEventDetectionSource.INPUT_METHOD,
            )

            recordedKeys.add(recordedKey)
            coroutineScope.launch {
                onRecordKey.emit(recordedKey)
            }
        }

        return true
    }

    private fun onRecordKey(recordedKey: RecordedKey) {
        recordedKeys.add(recordedKey)
        runBlocking { onRecordKey.emit(recordedKey) }
    }

    private fun createRecordedKey(
        keyCode: Int,
        device: InputDeviceInfo?,
        detectionSource: InputEventDetectionSource,
    ): RecordedKey {
        val triggerKeyDevice = if (device != null && device.isExternal) {
            TriggerKeyDevice.External(device.descriptor, device.name)
        } else {
            TriggerKeyDevice.Internal
        }

        return RecordedKey(keyCode, triggerKeyDevice, detectionSource)
    }

    private fun recordTriggerJob(): Job = coroutineScope.launch {
        repeat(RECORD_TRIGGER_TIMER_LENGTH) { iteration ->
            if (isActive) {
                val timeLeft = RECORD_TRIGGER_TIMER_LENGTH - iteration

                state.update { RecordTriggerState.CountingDown(timeLeft) }

                delay(1000)
            }
        }

        stopRecording()
    }

}

interface RecordTriggerController {
    val state: Flow<RecordTriggerState>
    val onRecordKey: Flow<RecordedKey>

    /**
     * @return Success if started and an Error if failed to start.
     */
    suspend fun startRecording(): KMResult<*>
    suspend fun stopRecording(): KMResult<*>
}
