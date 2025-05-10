package io.github.sds100.keymapper.system.accessibility

import kotlinx.serialization.Serializable

@Serializable
sealed class RecordAccessibilityNodeState {
    data object Idle : RecordAccessibilityNodeState()

    data class CountingDown(
        /**
         * The time left in seconds
         */
        val timeLeft: Int,
    ) : RecordAccessibilityNodeState()
}
