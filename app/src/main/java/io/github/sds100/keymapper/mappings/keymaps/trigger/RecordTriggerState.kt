package io.github.sds100.keymapper.mappings.keymaps.trigger

/**
 * Created by sds100 on 04/03/2021.
 */
sealed class RecordTriggerState {
    data class CountingDown(
        /**
         * The time left in seconds
         */
        val timeLeft: Int,
    ) : RecordTriggerState()

    object Stopped : RecordTriggerState()
}
