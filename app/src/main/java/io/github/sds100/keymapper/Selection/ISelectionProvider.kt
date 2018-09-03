package io.github.sds100.keymapper.Selection

/**
 * Created by sds100 on 13/08/2018.
 */
interface ISelectionProvider {
    val isSelecting: Boolean
    val selectionCount: Int

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
    fun unsubscribeToSelectionEvents(callback: SelectionCallback)
}