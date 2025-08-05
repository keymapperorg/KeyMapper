package io.github.sds100.keymapper.base.trigger

import android.view.KeyEvent
import io.github.sds100.keymapper.base.input.InputEventDetectionSource
import io.github.sds100.keymapper.base.keymaps.detection.DpadMotionEventTracker
import io.github.sds100.keymapper.common.utils.InputDeviceInfo
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.system.inputevents.KMGamePadEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordTriggerControllerImpl @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val serviceAdapter: AccessibilityServiceAdapter,
) : RecordTriggerController {
    override val state = MutableStateFlow<RecordTriggerState>(RecordTriggerState.Idle)

    private val recordedKeys: MutableList<RecordedKey> = mutableListOf()
    override val onRecordKey: MutableSharedFlow<RecordedKey> = MutableSharedFlow()
    private val dpadMotionEventTracker: DpadMotionEventTracker = DpadMotionEventTracker()

    init {
        serviceAdapter.eventReceiver.onEach { event ->
            when (event) {
                is RecordTriggerEvent.OnStoppedRecordingTrigger ->
                    state.value =
                        RecordTriggerState.Completed(recordedKeys)

                is RecordTriggerEvent.OnIncrementRecordTriggerTimer ->
                    state.value =
                        RecordTriggerState.CountingDown(event.timeLeft)

                else -> Unit
            }
        }.launchIn(coroutineScope)

        serviceAdapter.eventReceiver
            .mapNotNull {
                if (it is RecordTriggerEvent.RecordedTriggerKey) {
                    it
                } else {
                    null
                }
            }
            .map { createRecordedKeyEvent(it.keyCode, it.device, it.detectionSource) }
            .onEach { key ->
                recordedKeys.add(key)
                onRecordKey.emit(key)
            }
            .launchIn(coroutineScope)
    }

    override suspend fun startRecording(): KMResult<*> {
        recordedKeys.clear()
        dpadMotionEventTracker.reset()
        return serviceAdapter.send(RecordTriggerEvent.StartRecordingTrigger)
    }

    override suspend fun stopRecording(): KMResult<*> {
        return serviceAdapter.send(RecordTriggerEvent.StopRecordingTrigger)
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
            val recordedKey = createRecordedKeyEvent(
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

    private fun createRecordedKeyEvent(
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
