package io.github.sds100.keymapper.base.detection

import io.github.sds100.keymapper.base.actions.PerformActionsUseCase
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.base.input.InputEventDetectionSource
import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.input.InputEventHubCallback
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.base.trigger.AssistantTriggerType
import io.github.sds100.keymapper.base.trigger.EvdevTriggerKey
import io.github.sds100.keymapper.base.trigger.RecordTriggerController
import io.github.sds100.keymapper.base.trigger.RecordTriggerState
import io.github.sds100.keymapper.base.trigger.Trigger
import io.github.sds100.keymapper.system.inputevents.KMEvdevEvent
import io.github.sds100.keymapper.system.inputevents.KMInputEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class KeyMapDetectionController(
    private val coroutineScope: CoroutineScope,
    private val detectUseCase: DetectKeyMapsUseCase,
    private val performActionsUseCase: PerformActionsUseCase,
    private val detectConstraints: DetectConstraintsUseCase,
    private val inputEventHub: InputEventHub,
    private val pauseKeyMapsUseCase: PauseKeyMapsUseCase,
    private val recordTriggerController: RecordTriggerController,
) : InputEventHubCallback {
    companion object {
        private const val INPUT_EVENT_HUB_ID = "key_map_controller"
    }

    private val algorithm: KeyMapAlgorithm =
        KeyMapAlgorithm(coroutineScope, detectUseCase, performActionsUseCase, detectConstraints)

    private val isPaused: StateFlow<Boolean> =
        pauseKeyMapsUseCase.isPaused.stateIn(coroutineScope, SharingStarted.Eagerly, false)

    init {
        // Must first register before collecting anything that may call reset()
        inputEventHub.registerClient(INPUT_EVENT_HUB_ID, this, listOf(KMEvdevEvent.TYPE_KEY_EVENT))

        coroutineScope.launch {
            combine(detectUseCase.allKeyMapList, isPaused) { keyMapList, isPaused ->
                algorithm.reset()

                if (isPaused) {
                    algorithm.loadKeyMaps(emptyList())
                    inputEventHub.setGrabbedEvdevDevices(INPUT_EVENT_HUB_ID, emptyList())
                } else {
                    algorithm.loadKeyMaps(keyMapList)
                    // Only grab the triggers that are actually being listened to by the algorithm
                    grabEvdevDevicesForTriggers(algorithm.triggers)
                }
            }.launchIn(coroutineScope)
        }
    }

    override fun onInputEvent(
        event: KMInputEvent,
        detectionSource: InputEventDetectionSource,
    ): Boolean {
        if (isPaused.value) {
            return false
        }

        if (recordTriggerController.state.value is RecordTriggerState.CountingDown) {
            return false
        }

        return algorithm.onInputEvent(event)
    }

    fun onFingerprintGesture(type: FingerprintGestureType) {
        algorithm.onFingerprintGesture(type)
    }

    fun onFloatingButtonDown(button: String) {
        algorithm.onFloatingButtonDown(button)
    }

    fun onFloatingButtonUp(button: String) {
        algorithm.onFloatingButtonUp(button)
    }

    fun onAssistantEvent(event: AssistantTriggerType) {
        algorithm.onAssistantEvent(event)
    }

    fun teardown() {
        reset()
        inputEventHub.unregisterClient(INPUT_EVENT_HUB_ID)
    }

    private fun grabEvdevDevicesForTriggers(triggers: Array<Trigger>) {
        val evdevDevices = triggers
            .flatMap { trigger -> trigger.keys.filterIsInstance<EvdevTriggerKey>() }
            .map { it.device }
            .distinct()
            .toList()

        Timber.i("Grab evdev devices for key map detection: ${evdevDevices.joinToString()}")
        inputEventHub.setGrabbedEvdevDevices(
            INPUT_EVENT_HUB_ID,
            evdevDevices,
        )
    }

    private fun reset() {
        algorithm.reset()
        inputEventHub.setGrabbedEvdevDevices(INPUT_EVENT_HUB_ID, emptyList())
    }
}
