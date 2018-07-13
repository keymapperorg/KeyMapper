package io.github.sds100.keymapper

/**
 * Created by sds100 on 12/07/2018.
 */

interface SelectionCallback<K> {
    fun onItemSelected(id: K)
    fun onItemUnselected(id: K)
}