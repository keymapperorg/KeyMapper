package io.github.sds100.keymapper.base.keymaps.detection

import io.github.sds100.keymapper.base.actions.PerformActionsUseCase
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.base.input.InputEventDetectionSource
import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.input.InputEventHubCallback
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.system.inputevents.KMInputEvent
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeyMapDetectionController(
    private val coroutineScope: CoroutineScope,
    private val detectUseCase: DetectKeyMapsUseCase,
    private val performActionsUseCase: PerformActionsUseCase,
    private val detectConstraints: DetectConstraintsUseCase,
    private val inputEventHub: InputEventHub,
    private val pauseKeyMapsUseCase: PauseKeyMapsUseCase
) : InputEventHubCallback {
    companion object {
        private const val INPUT_EVENT_HUB_ID = "key_map_controller"
    }

    private val algorithm: KeyMapAlgorithm =
        KeyMapAlgorithm(coroutineScope, detectUseCase, performActionsUseCase, detectConstraints)

    private val isPaused: StateFlow<Boolean> =
        pauseKeyMapsUseCase.isPaused.stateIn(coroutineScope, SharingStarted.Eagerly, true)

    init {
        coroutineScope.launch {
            detectUseCase.allKeyMapList.collectLatest { keyMapList ->
                algorithm.reset()
                algorithm.loadKeyMaps(keyMapList)
            }
        }

        coroutineScope.launch {
            isPaused.collect { isPaused ->
                if (isPaused) {
                    reset()
                }
            }
        }

        inputEventHub.registerClient(INPUT_EVENT_HUB_ID, this)
    }

    override fun onInputEvent(
        event: KMInputEvent,
        detectionSource: InputEventDetectionSource
    ): Boolean {
        if (isPaused.value) {
            return false
        }

        if (event is KMKeyEvent) {
            return algorithm.onKeyEvent(event)
        } else {
            return false
        }
    }

    fun onFingerprintGesture(type: FingerprintGestureType) {
        algorithm.onFingerprintGesture(type)
    }

    fun teardown() {
        reset()
        inputEventHub.unregisterClient(INPUT_EVENT_HUB_ID)
    }

    private fun reset() {
        algorithm.reset()
        inputEventHub.setGrabbedEvdevDevices(INPUT_EVENT_HUB_ID, emptyList())
    }
}