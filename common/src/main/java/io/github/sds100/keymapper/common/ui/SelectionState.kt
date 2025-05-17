package io.github.sds100.keymapper.common.ui


sealed class SelectionState {
    data class Selecting(val selectedIds: Set<String>) : SelectionState()
    data object NotSelecting : SelectionState()
}
