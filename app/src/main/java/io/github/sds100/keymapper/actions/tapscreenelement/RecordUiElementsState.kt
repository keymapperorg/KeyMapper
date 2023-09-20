package io.github.sds100.keymapper.actions.tapscreenelement

/**
 * Created by sds100 on 04/03/2021.
 */
sealed class RecordUiElementsState {
    data class CountingDown(
        /**
         * The time left in seconds
         */
        val timeLeft: Int
    ) : RecordUiElementsState()

    object Stopped : RecordUiElementsState()
}