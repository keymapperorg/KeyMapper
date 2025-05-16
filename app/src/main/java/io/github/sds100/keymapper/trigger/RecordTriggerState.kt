package io.github.sds100.keymapper.trigger


sealed class RecordTriggerState {
    data object Idle : RecordTriggerState()

    data class CountingDown(
        /**
         * The time left in seconds
         */
        val timeLeft: Int,
    ) : RecordTriggerState()

    data class Completed(val recordedKeys: List<RecordedKey>) : RecordTriggerState()
}
