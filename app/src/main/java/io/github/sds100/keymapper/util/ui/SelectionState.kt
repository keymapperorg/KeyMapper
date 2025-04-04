package io.github.sds100.keymapper.util.ui

/**
 * Created by sds100 on 22/03/2021.
 */
sealed class SelectionState {
    data class Selecting(val selectedIds: Set<String>) : SelectionState()
    data object NotSelecting : SelectionState()
}
