package io.github.sds100.keymapper.util.ui

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Created by sds100 on 11/02/2020.
 */

class MultiSelectProviderImpl<T> : MultiSelectProvider<T> {
    private val lock = Any()
    override val state = MutableStateFlow<SelectionState>(SelectionState.NotSelecting)

    override fun startSelecting(): Boolean {
        if (state.value !is SelectionState.Selecting<*>) {
            state.value = SelectionState.Selecting(emptySet<T>())

            return true
        }

        return false
    }

    override fun stopSelecting() {
        state.value = SelectionState.NotSelecting
    }

    override fun toggleSelection(id: T) {
        if (state.value !is SelectionState.Selecting<*>) return

        if ((state.value as SelectionState.Selecting<*>).selectedIds.contains(id)) {
            deselect(id)
        } else {
            select(id)
        }
    }

    override fun isSelected(id: T): Boolean = state.value is SelectionState.Selecting<*> &&
        (state.value as SelectionState.Selecting<*>).selectedIds.contains(id)

    override fun isSelecting(): Boolean = state.value is SelectionState.Selecting<*>

    override fun getSelectedIds(): Set<T> {
        val selectionState = state.value

        if (selectionState is SelectionState.Selecting<*>) {
            return selectionState.selectedIds as Set<T>
        } else {
            return emptySet()
        }
    }

    override fun select(vararg id: T) {
        synchronized(lock) {
            state.value.apply {
                if (this !is SelectionState.Selecting<*>) return

                state.value = SelectionState.Selecting(selectedIds.plus(id))
            }
        }
    }

    override fun deselect(vararg id: T) {
        synchronized(lock) {
            state.value.apply {
                if (this !is SelectionState.Selecting<*>) return

                state.value = SelectionState.Selecting(selectedIds.minus(id))
            }
        }
    }

    override fun reset() {
        state.value = SelectionState.NotSelecting
    }
}
