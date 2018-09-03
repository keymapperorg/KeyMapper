package io.github.sds100.keymapper.Selection

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

    override val selectionCount: Int
        get() = mSelectedItems.size

    override val isSelecting: Boolean
        get() = mIsSelecting

    private var mIsSelecting = false

    /**
     * Stores the ids of the selected items
     */
    private val mSelectedItems: MutableList<Long> = mutableListOf()

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

    //TODO onSaveInstanceState and onRestoreInstanceState
}