package io.github.sds100.keymapper.base.input

import io.github.sds100.keymapper.system.inputevents.KMInputEvent

interface InputEventHubCallback {
    /**
     * @return whether to consume the event.
     */
    fun onInputEvent(
        event: KMInputEvent,
        detectionSource: InputEventDetectionSource,
    ): Boolean
}
