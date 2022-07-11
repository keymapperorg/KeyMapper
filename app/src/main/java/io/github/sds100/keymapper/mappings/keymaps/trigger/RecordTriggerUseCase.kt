package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Created by sds100 on 04/03/2021.
 */
class RecordTriggerUseCaseImpl @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val accessibilityServiceAdapter: AccessibilityServiceAdapter
) : RecordTriggerUseCase {
    override val state = MutableStateFlow<RecordTriggerState>(RecordTriggerState.Stopped)

    override val onRecordKey = accessibilityServiceAdapter.eventReceiver.mapNotNull { event ->
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
        accessibilityServiceAdapter.eventReceiver.onEach { event ->
            when (event) {
                is Event.OnStoppedRecordingTrigger -> state.value = RecordTriggerState.Stopped

                is Event.OnIncrementRecordTriggerTimer -> state.value =
                    RecordTriggerState.CountingDown(event.timeLeft)
            }
        }.launchIn(coroutineScope)
    }

    override suspend fun startRecording(): Result<*> {
        return accessibilityServiceAdapter.send(Event.StartRecordingTrigger)
    }

    override suspend fun stopRecording(): Result<*> {
        return accessibilityServiceAdapter.send(Event.StopRecordingTrigger)
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