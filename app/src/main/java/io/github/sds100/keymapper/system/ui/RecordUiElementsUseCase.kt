package io.github.sds100.keymapper.system.ui

import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class RecordUiElementsController(
    private val coroutineScope: CoroutineScope,
    private val serviceAdapter: ServiceAdapter,
) : RecordUiElementsUseCase {
    override val state = MutableStateFlow<RecordUiElementsState>(RecordUiElementsState.Stopped)
    override val uiElements: MutableStateFlow<List<UiElementInfo>> = MutableStateFlow(emptyList())

    init {
        serviceAdapter.eventReceiver.onEach { event ->
            when (event) {
                is Event.OnStoppedRecordingUiElements -> state.value = RecordUiElementsState.Stopped
                is Event.OnIncrementRecordUiElementsTimer ->
                    state.value =
                        RecordUiElementsState.CountingDown(event.timeLeft)

                is Event.OnRecordUiElement -> onRecordUiElement(event.element)
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

    private fun onRecordUiElement(element: UiElementInfo) {
        uiElements.update { old ->
            if (old.none { it.fullName == element.fullName }) {
                old.plus(element)
            } else {
                old
            }
        }
    }

    override fun clearElements() {
        uiElements.value = emptyList()
    }
}

interface RecordUiElementsUseCase {
    val state: Flow<RecordUiElementsState>
    val uiElements: StateFlow<List<UiElementInfo>>

    /**
     * @return Success if started and an Error if failed to start.
     */
    suspend fun startRecording(): Result<*>
    suspend fun stopRecording(): Result<*>
    fun clearElements()
}
