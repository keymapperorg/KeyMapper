package io.github.sds100.keymapper.system.ui

sealed class RecordUiElementsState {
    data class CountingDown(
        /**
         * The time left in seconds
         */
        val timeLeft: Int,
    ) : RecordUiElementsState()

    object Stopped : RecordUiElementsState()
}
