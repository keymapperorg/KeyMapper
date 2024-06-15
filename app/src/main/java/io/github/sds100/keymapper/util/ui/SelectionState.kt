package io.github.sds100.keymapper.util.ui

/**
 * Created by sds100 on 22/03/2021.
 */
sealed class SelectionState {
    data class Selecting<T>(val selectedIds: Set<T>) : SelectionState()
    object NotSelecting : SelectionState()
}
