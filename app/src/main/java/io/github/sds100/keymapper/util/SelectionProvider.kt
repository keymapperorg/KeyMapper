package io.github.sds100.keymapper.util

import android.util.SparseBooleanArray
import androidx.core.util.forEach
import androidx.core.util.set
import androidx.core.util.size
import androidx.lifecycle.MutableLiveData
import io.github.sds100.keymapper.ui.callback.SelectionCallback

/**
 * Created by sds100 on 11/02/2020.
 */

class SelectionProvider : ISelectionProvider {
    override val isSelectable: MutableLiveData<Boolean> = MutableLiveData(false)
    override val selectedCount: MutableLiveData<Int> = MutableLiveData(0)

    private var _selectedIds = SparseBooleanArray()
    override val selectedIds: LongArray
        get() = sequence {
            _selectedIds.forEach { id, selected ->
                if (selected) {
                    yield(id.toLong())
                }
            }
        }.toList().toLongArray()

    override var callback: SelectionCallback? = null

    override fun startSelecting(): Boolean {
        if (isSelectable.value == false) {
            unselectAll()

            isSelectable.value = true

            return true
        }

        return false
    }

    override fun stopSelecting() {
        if (isSelectable.value == true) {
            isSelectable.value = false
        }

        unselectAll()
    }

    override fun toggleSelection(id: Long) {
        _selectedIds[id.toInt()] = !isSelected(id)

        if (isSelected(id)) {
            callback?.onSelect(id)
            selectedCount.value = selectedCount.value?.plus(1)
        } else {
            callback?.onUnselect(id)
            selectedCount.value = selectedCount.value?.minus(1)
        }
    }

    override fun isSelected(id: Long): Boolean {
        return _selectedIds[id.toInt()]
    }

    override fun updateIds(ids: LongArray) {
        val newSparseArray = SparseBooleanArray()

        ids.forEach {
            val id = it.toInt()
            val value = _selectedIds[id]

            newSparseArray.put(id, value)
        }

        _selectedIds = newSparseArray
    }

    override fun selectAll() {
        _selectedIds.forEach { key, _ ->
            _selectedIds[key] = true
        }
        selectedCount.value = _selectedIds.size
        callback?.onSelectAll()
    }

    private fun unselectAll() {
        //unselect all the items
        _selectedIds.forEach { key, _ ->
            _selectedIds[key] = false
        }
        selectedCount.value = 0
    }
}