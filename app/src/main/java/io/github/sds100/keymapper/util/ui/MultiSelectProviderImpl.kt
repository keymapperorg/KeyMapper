package io.github.sds100.keymapper.util.ui

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Created by sds100 on 11/02/2020.
 */

class MultiSelectProviderImpl : MultiSelectProvider {
    private val lock = Any()
    override val state = MutableStateFlow<SelectionState>(SelectionState.NotSelecting)

    override fun startSelecting(): Boolean {
        if (state.value !is SelectionState.Selecting) {
            state.value = SelectionState.Selecting(emptySet())

            return true
        }

        return false
    }

    override fun stopSelecting() {
        if (state.value is SelectionState.Selecting) {
            state.value = SelectionState.NotSelecting
        }
    }

    override fun toggleSelection(id: String) {
        if (state.value !is SelectionState.Selecting) return

        if ((state.value as SelectionState.Selecting).selectedIds.contains(id)) {
            deselect(id)
        } else {
            select(id)
        }
    }

    override fun isSelected(id: String): Boolean {
        return state.value is SelectionState.Selecting
            && (state.value as SelectionState.Selecting).selectedIds.contains(id)
    }

    override fun select(vararg id: String) {
        synchronized(lock) {
            state.value.apply {
                if (this !is SelectionState.Selecting) return

                state.value = SelectionState.Selecting(selectedIds.plus(id))
            }
        }
    }

    override fun deselect(vararg id: String) {
        synchronized(lock) {
            state.value.apply {
                if (this !is SelectionState.Selecting) return

                state.value = SelectionState.Selecting(selectedIds.minus(id))
            }
        }
    }

    override fun reset() {
        state.value = SelectionState.NotSelecting
    }
}