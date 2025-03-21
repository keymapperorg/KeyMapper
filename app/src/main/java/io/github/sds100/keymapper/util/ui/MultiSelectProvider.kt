package io.github.sds100.keymapper.util.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Created by sds100 on 11/02/2020.
 */

class MultiSelectProvider {
    private val lock = Any()
    val state = MutableStateFlow<SelectionState>(SelectionState.NotSelecting)

    fun startSelecting(): Boolean {
        if (state.value !is SelectionState.Selecting) {
            state.update { SelectionState.Selecting(emptySet()) }

            return true
        }

        return false
    }

    fun stopSelecting() {
        state.update { SelectionState.NotSelecting }
    }

    fun toggleSelection(id: String) {
        if (state.value !is SelectionState.Selecting) return

        if ((state.value as SelectionState.Selecting).selectedIds.contains(id)) {
            deselect(id)
        } else {
            select(id)
        }
    }

    fun isSelected(id: String): Boolean = state.value is SelectionState.Selecting &&
        (state.value as SelectionState.Selecting).selectedIds.contains(
            id,
        )

    fun isSelecting(): Boolean = state.value is SelectionState.Selecting

    fun getSelectedIds(): Set<String> {
        val selectionState = state.value

        if (selectionState is SelectionState.Selecting) {
            return selectionState.selectedIds
        } else {
            return emptySet()
        }
    }

    fun select(vararg id: String) {
        synchronized(lock) {
            state.value.apply {
                if (this !is SelectionState.Selecting) return

                state.value = SelectionState.Selecting(selectedIds.plus(id))
            }
        }
    }

    fun deselect(vararg id: String) {
        synchronized(lock) {
            state.value.apply {
                if (this !is SelectionState.Selecting) return

                state.value = SelectionState.Selecting(selectedIds.minus(id))
            }
        }
    }

    fun clear() {
        state.update { state ->
            if (state is SelectionState.Selecting) {
                state.copy(selectedIds = emptySet())
            } else {
                state
            }
        }
    }

    fun reset() {
        state.value = SelectionState.NotSelecting
    }
}
