package io.github.sds100.keymapper.Selection

import android.os.Bundle

/**
 * Created by sds100 on 12/07/2018.
 */

/**
 * Controls multi-selecting items in a list. Classes can subscribe to selection events with
 * [SelectionCallback]
 *
 * @see [SelectionCallback]
 */
class SelectionProvider : ISelectionProvider {

    companion object {
        const val KEY_SELECTION_PROVIDER_STATE = "selection_provider_state"

        private const val KEY_SELECTED_ITEMS = "selected_items"
    }

    override val selectionCount: Int
        get() = mSelectedItems.size

    override val inSelectingMode: Boolean
        get() = mIsSelecting

    private var mIsSelecting = false

    /**
     * Stores the ids of the selected items
     */
    private var mSelectedItems: MutableList<Long> = mutableListOf()

    /**
     * All the classes which have subscribed to receive selection events. Such as when an item
     * is selected and unselected
     * @see SelectionCallback
     */
    private val mSelectionCallbacks: MutableList<SelectionCallback> = mutableListOf()

    override fun toggleSelection(itemId: Long) {
        if (mSelectedItems.contains(itemId)) {
            mSelectedItems.remove(itemId)
            mSelectionCallbacks.forEach { it.onItemUnselected(itemId) }
        } else {

            //if it is the first item to be selected send onStartMultiSelect event
            if (!mIsSelecting) {
                mIsSelecting = true
                mSelectionCallbacks.forEach { it.onStartMultiSelect() }
            }

            mSelectedItems.add(itemId)
            mSelectionCallbacks.forEach { it.onItemSelected(itemId) }
        }
    }

    override fun stopSelecting() {
        mIsSelecting = false
        mSelectedItems.clear()

        mSelectionCallbacks.forEach { it.onStopMultiSelect() }
    }

    override fun isSelected(itemId: Long): Boolean {
        return mSelectedItems.any { it == itemId }
    }

    override fun subscribeToSelectionEvents(callback: SelectionCallback) {
        mSelectionCallbacks.add(callback)
    }

    override fun unsubscribeToSelectionEvents(callback: SelectionCallback) {
        mSelectionCallbacks.remove(callback)
    }

    override fun saveInstanceState(): Bundle {
        return Bundle().apply {
            //only save state if the user is selecting items
            if (inSelectingMode) {
                putLongArray(KEY_SELECTED_ITEMS, mSelectedItems.toLongArray())
            }
        }
    }

    override fun restoreInstanceState(bundle: Bundle) {
        if (bundle.containsKey(KEY_SELECTED_ITEMS)) {
            mIsSelecting = true
            mSelectedItems = bundle.getLongArray(KEY_SELECTED_ITEMS)!!.toMutableList()

            mSelectionCallbacks.forEach {
                it.onStartMultiSelect()
            }
        }
    }
}