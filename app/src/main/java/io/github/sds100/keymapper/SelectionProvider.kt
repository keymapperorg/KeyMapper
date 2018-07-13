package io.github.sds100.keymapper

/**
 * Created by sds100 on 12/07/2018.
 */

/**
 * @param K The data type of the unique identifier for each item
 */
class SelectionProvider<K> {
    /**
     * Stores the ids of the selected items
     */
    private val mSelectedItems: MutableList<K> = mutableListOf()

    /**
     * All the classes which have subscribed to receive selection events. Such as when an item
     * is selected and unselected
     * @see SelectionCallback
     */
    private val mSelectionCallbacks: MutableList<SelectionCallback<K>> = mutableListOf()

    /**
     * @param itemId The id of the itemId to toggle
     */
    fun toggleSelection(itemId: K) {
        if (mSelectedItems.contains(itemId)) {
            mSelectedItems.remove(itemId)
            mSelectionCallbacks.forEach { it.onItemUnselected(itemId) }
        } else {
            mSelectedItems.add(itemId)
            mSelectionCallbacks.forEach { it.onItemSelected(itemId) }
        }
    }

    /**
     * @return whether the item is selected.
     */
    fun isSelected(itemId: K): Boolean {
        return mSelectedItems.any { it == itemId }
    }

    /**
     * Subscribe with a [SelectionCallback] to receive selection events.
     */
    fun subscribeToSelectionEvents(callback: SelectionCallback<K>) {
        mSelectionCallbacks.add(callback)
    }
}