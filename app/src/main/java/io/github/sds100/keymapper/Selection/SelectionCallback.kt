package io.github.sds100.keymapper.Selection

/**
 * Created by sds100 on 12/07/2018.
 */

interface SelectionCallback {
    fun onStartMultiSelect()
    fun onStopMultiSelect()
    fun onItemSelected(id: Long)
    fun onItemUnselected(id: Long)
    fun onSelectAll()
}