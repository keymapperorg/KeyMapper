package io.github.sds100.keymapper.util.ui

import kotlinx.coroutines.flow.StateFlow

/**
 * Created by sds100 on 11/02/2020.
 */

interface MultiSelectProvider<T> {
    val state: StateFlow<SelectionState>

    /**
     * @return true if it wasn't already selecting
     */
    fun startSelecting(): Boolean

    fun stopSelecting()

    fun toggleSelection(id: T)

    fun select(vararg id: T)
    fun deselect(vararg id: T)

    fun isSelected(id: T): Boolean
    fun getSelectedIds(): Set<T>
    fun isSelecting(): Boolean

    fun reset()
}
