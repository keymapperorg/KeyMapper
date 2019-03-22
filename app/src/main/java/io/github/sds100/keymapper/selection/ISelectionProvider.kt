package io.github.sds100.keymapper.selection

import android.os.Bundle

/**
 * Created by sds100 on 13/08/2018.
 */
interface ISelectionProvider {
    val inSelectingMode: Boolean
    val selectionCount: Int
    val selectedItemIds: LongArray

    /**
     * A list of all the ids for all the items which can be selected
     */
    var allItemIds: List<Long>

    /**
     * @return whether the item is selected.
     */
    fun isSelected(itemId: Long): Boolean

    /**
     * Stop selecting items
     */
    fun stopSelecting()

    /**
     * @param itemId The id of the itemId to toggle
     */
    fun toggleSelection(itemId: Long)

    /**
     * Subscribe with a [SelectionCallback] to receive selection events.
     */
    fun subscribeToSelectionEvents(callback: SelectionCallback)

    /**
     * Unsubscribe a [SelectionCallback] from receiving selection events
     */
    fun unsubscribeFromSelectionEvents(callback: SelectionCallback)

    fun selectAll()

    fun saveInstanceState(): Bundle
    fun restoreInstanceState(bundle: Bundle)
}