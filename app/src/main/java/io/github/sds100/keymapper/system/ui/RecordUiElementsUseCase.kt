package io.github.sds100.keymapper.system.ui

import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class RecordUiElementsController(
    private val coroutineScope: CoroutineScope,
    private val serviceAdapter: ServiceAdapter
) : RecordUiElementsUseCase {
    override val state = MutableStateFlow<RecordUiElementsState>(RecordUiElementsState.Stopped)

    init {
        serviceAdapter.eventReceiver.onEach { event ->
            when (event) {
                is Event.OnStoppedRecordingUiElements -> state.value = RecordUiElementsState.Stopped
                is Event.OnIncrementRecordUiElementsTimer -> state.value =
                    RecordUiElementsState.CountingDown(event.timeLeft)
                else -> Unit
            }
        }.launchIn(coroutineScope)
    }

    override suspend fun startRecording(): Result<*> {
        return serviceAdapter.send(Event.StartRecordingUiElements)
    }

    override suspend fun stopRecording(): Result<*> {
        return serviceAdapter.send(Event.StopRecordingUiElements)
    }
}

interface RecordUiElementsUseCase {
    val state: Flow<RecordUiElementsState>

    /**
     * @return Success if started and an Error if failed to start.
     */
    suspend fun startRecording(): Result<*>
    suspend fun stopRecording(): Result<*>
}