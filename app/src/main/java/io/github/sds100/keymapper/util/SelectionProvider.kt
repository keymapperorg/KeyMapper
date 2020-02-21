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

    override val selectedIds: LongArray
        get() = sequence {
            mSelectedIds.forEach { id, selected ->
                if (selected) {
                    yield(id.toLong())
                }
            }
        }.toList().toLongArray()

    override var callback: SelectionCallback? = null

    private var mSelectedIds = SparseBooleanArray()

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
        mSelectedIds[id.toInt()] = !isSelected(id)

        if (isSelected(id)) {
            callback?.onSelect(id)
            selectedCount.value = selectedCount.value?.plus(1)
        } else {
            callback?.onUnselect(id)
            selectedCount.value = selectedCount.value?.minus(1)
        }
    }

    override fun isSelected(id: Long): Boolean {
        return mSelectedIds[id.toInt()]
    }

    override fun updateIds(ids: LongArray) {
        val newSparseArray = SparseBooleanArray()

        ids.forEach {
            val id = it.toInt()
            val value = mSelectedIds[id]

            newSparseArray.put(id, value)
        }

        mSelectedIds = newSparseArray
    }

    override fun selectAll() {
        mSelectedIds.forEach { key, _ ->
            mSelectedIds[key] = true
        }
        selectedCount.value = mSelectedIds.size
        callback?.onSelectAll()
    }

    private fun unselectAll() {
        //unselect all the items
        mSelectedIds.forEach { key, _ ->
            mSelectedIds[key] = false
        }
        selectedCount.value = 0
    }
}