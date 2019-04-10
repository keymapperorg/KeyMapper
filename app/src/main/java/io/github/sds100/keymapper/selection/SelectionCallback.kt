package io.github.sds100.keymapper.selection

/**
 * Created by sds100 on 12/07/2018.
 */

interface SelectionCallback {
    /**
     * @param id the id of the item affected by the selection event. It's null if all items are
     * affected
     */
    fun onSelectionEvent(id: Long? = null, event: SelectionEvent)
}