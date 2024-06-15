package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

/**
 * Created by sds100 on 04/03/2021.
 */
class RecordTriggerController(
    private val coroutineScope: CoroutineScope,
    private val serviceAdapter: ServiceAdapter,
) : RecordTriggerUseCase {
    override val state = MutableStateFlow<RecordTriggerState>(RecordTriggerState.Stopped)

    override val onRecordKey = serviceAdapter.eventReceiver.mapNotNull { event ->
        when (event) {
            is Event.RecordedTriggerKey -> {
                val device = if (event.device != null && event.device.isExternal) {
                    TriggerKeyDevice.External(event.device.descriptor, event.device.name)
                } else {
                    TriggerKeyDevice.Internal
                }

                RecordedKey(event.keyCode, device)
            }

            else -> null
        }
    }

    init {
        serviceAdapter.eventReceiver.onEach { event ->
            when (event) {
                is Event.OnStoppedRecordingTrigger -> state.value = RecordTriggerState.Stopped

                is Event.OnIncrementRecordTriggerTimer ->
                    state.value =
                        RecordTriggerState.CountingDown(event.timeLeft)

                else -> Unit
            }
        }.launchIn(coroutineScope)
    }

    override suspend fun startRecording(): Result<*> =
        serviceAdapter.send(Event.StartRecordingTrigger)

    override suspend fun stopRecording(): Result<*> =
        serviceAdapter.send(Event.StopRecordingTrigger)
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
