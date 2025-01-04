package io.github.sds100.keymapper.mappings.keymaps.trigger

import android.view.KeyEvent
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.ServiceEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 04/03/2021.
 */
class RecordTriggerController(
    private val coroutineScope: CoroutineScope,
    private val serviceAdapter: ServiceAdapter,
) : RecordTriggerUseCase {
    override val state = MutableStateFlow<RecordTriggerState>(RecordTriggerState.Stopped)

    override val onRecordKey: MutableSharedFlow<RecordedKey> = MutableSharedFlow()

    init {
        serviceAdapter.eventReceiver.onEach { event ->
            when (event) {
                is ServiceEvent.OnStoppedRecordingTrigger ->
                    state.value =
                        RecordTriggerState.Stopped

                is ServiceEvent.OnIncrementRecordTriggerTimer ->
                    state.value =
                        RecordTriggerState.CountingDown(event.timeLeft)

                else -> Unit
            }
        }.launchIn(coroutineScope)

        serviceAdapter.eventReceiver
            .mapNotNull {
                if (it is ServiceEvent.RecordedTriggerKey) {
                    it
                } else {
                    null
                }
            }
            .map { createRecordedKeyEvent(it.keyCode, it.device, it.detectionSource) }
            .onEach { key -> onRecordKey.emit(key) }
            .launchIn(coroutineScope)
    }

    override suspend fun startRecording(): Result<*> =
        serviceAdapter.send(ServiceEvent.StartRecordingTrigger)

    override suspend fun stopRecording(): Result<*> =
        serviceAdapter.send(ServiceEvent.StopRecordingTrigger)

    /**
     * Process key events from the activity so that DPAD buttons can be recorded
     * even when the Key Mapper IME is not being used.
     * @return Whether the key event is consumed.
     */
    fun onRecordKeyFromActivity(event: KeyEvent): Boolean {
        // Only consume the key event if the app is recording a trigger.
        if (state.value == RecordTriggerState.Stopped) {
            return false
        }

        if (!KeyEventUtils.isDpadKeyCode(event.keyCode)) {
            return false
        }

        // If it is the up key event then consume it and record the key. Do not record the key
        // on the down event because the down key event may be consumed by a View and so
        // onKeyDown will not be called in the activity. This can be reproduced by navigating
        // to the TriggerFragment by touch and then pressing a DPAD button will cause the View
        // consume the key event and get focus.
        if (event.action == KeyEvent.ACTION_UP) {

            val device = InputDeviceUtils.createInputDeviceInfo(event.device)
            val recordedKey = createRecordedKeyEvent(
                event.keyCode,
                device,
                KeyEventDetectionSource.INPUT_METHOD
            )

            coroutineScope.launch {
                onRecordKey.emit(recordedKey)
            }
        }

        return true
    }

    private fun createRecordedKeyEvent(
        keyCode: Int,
        device: InputDeviceInfo?,
        detectionSource: KeyEventDetectionSource
    ): RecordedKey {
        val triggerKeyDevice = if (device != null && device.isExternal) {
            TriggerKeyDevice.External(device.descriptor, device.name)
        } else {
            TriggerKeyDevice.Internal
        }

        return RecordedKey(keyCode, triggerKeyDevice, detectionSource)
    }
}

interface RecordTriggerUseCase {
    val state: Flow<RecordTriggerState>
    val onRecordKey: Flow<RecordedKey>

    /**
     * @return Success if started and an Error if failed to start.
     */
    suspend fun startRecording(): Result<*>
    suspend fun stopRecording(): Result<*>
}
