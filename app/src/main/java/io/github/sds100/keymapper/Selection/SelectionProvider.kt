package io.github.sds100.keymapper.selection

import android.os.Bundle

/**
 * Created by sds100 on 12/07/2018.
 */

/**
 * Controls multi-selecting items in a list. Classes can subscribe to selection events with
 * [SelectionCallback]
 *
 *
 * @see [SelectionCallback]
 */
class SelectionProvider(override var allItemIds: List<Long> = listOf()) : ISelectionProvider {

    companion object {
        const val KEY_SELECTION_PROVIDER_STATE = "selection_provider_state"

        private const val KEY_SELECTED_ITEMS = "selected_items"
    }

    override val selectionCount: Int
        get() = mSelectedItemIds.size

    override val inSelectingMode: Boolean
        get() = mIsSelecting

    override val selectedItemIds: LongArray
        get() = mSelectedItemIds.toLongArray()

    private var mIsSelecting = false

    /**
     * Stores the ids of the selected items
     */
    private var mSelectedItemIds: MutableList<Long> = mutableListOf()

    /**
     * All the classes which have subscribed to receive selection events. Such as when an item
     * is selected and unselected
     * @see SelectionCallback
     */
    private val mSelectionCallbacks: MutableList<SelectionCallback> = mutableListOf()

    override fun toggleSelection(itemId: Long) {
        if (mSelectedItemIds.contains(itemId)) {
            mSelectedItemIds.remove(itemId)
            mSelectionCallbacks.forEach { it.onSelectionEvent(itemId, SelectionEvent.UNSELECTED) }
        } else {

            //if it is the first item to be selected send onStartMultiSelect event
            if (!mIsSelecting) {
                mIsSelecting = true
                mSelectionCallbacks.forEach { it.onSelectionEvent(event = SelectionEvent.START) }
            }

            mSelectedItemIds.add(itemId)
            mSelectionCallbacks.forEach { it.onSelectionEvent(itemId, SelectionEvent.SELECTED) }
        }
    }

    override fun selectAll() {
        if (mIsSelecting) {
            mSelectedItemIds = allItemIds.toMutableList()

            mSelectionCallbacks.forEach { it.onSelectionEvent(event = SelectionEvent.SELECT_ALL) }
        }
    }

    override fun stopSelecting() {
        mIsSelecting = false
        mSelectedItemIds.clear()

        mSelectionCallbacks.forEach { it.onSelectionEvent(event = SelectionEvent.STOP) }
    }

    override fun isSelected(itemId: Long): Boolean {
        return mSelectedItemIds.any { it == itemId }
    }

    override fun subscribeToSelectionEvents(callback: SelectionCallback) {
        mSelectionCallbacks.add(callback)
    }

    override fun unsubscribeFromSelectionEvents(callback: SelectionCallback) {
        mSelectionCallbacks.remove(callback)
    }

    override fun saveInstanceState(): Bundle {
        return Bundle().apply {
            //only save state if the user is selecting items
            if (inSelectingMode) {
                putLongArray(KEY_SELECTED_ITEMS, mSelectedItemIds.toLongArray())
            }
        }
    }

    override fun restoreInstanceState(bundle: Bundle) {
        if (bundle.containsKey(KEY_SELECTED_ITEMS)) {
            mIsSelecting = true
            mSelectedItemIds = bundle.getLongArray(KEY_SELECTED_ITEMS)!!.toMutableList()

            mSelectionCallbacks.forEach {
                it.onSelectionEvent(event = SelectionEvent.START)
            }
        }
    }
}