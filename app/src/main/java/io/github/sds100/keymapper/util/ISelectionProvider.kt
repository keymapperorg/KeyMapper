package io.github.sds100.keymapper.util

import androidx.lifecycle.LiveData
import io.github.sds100.keymapper.ui.callback.SelectionCallback

/**
 * Created by sds100 on 11/02/2020.
 */

interface ISelectionProvider {
    var callback: SelectionCallback?

    val isSelectable: LiveData<Boolean>
    val selectedCount: LiveData<Int>
    val selectedIds: LongArray

    /**
     * @return true if it wasn't already selecting
     */
    fun startSelecting(): Boolean

    fun stopSelecting()

    fun toggleSelection(id: Long)

    fun selectAll()

    fun isSelected(id: Long): Boolean

    fun updateIds(ids: LongArray)
}