package io.github.sds100.keymapper.base.system.accessibility

import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent
import kotlinx.serialization.Serializable

sealed class RecordAccessibilityNodeEvent : AccessibilityServiceEvent() {
    @Serializable
    data object StartRecordingNodes : RecordAccessibilityNodeEvent()

    @Serializable
    data object StopRecordingNodes : RecordAccessibilityNodeEvent()

    @Serializable
    data class OnRecordNodeStateChanged(
        val state: RecordAccessibilityNodeState,
    ) : RecordAccessibilityNodeEvent()
}
