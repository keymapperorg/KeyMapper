package io.github.sds100.keymapper.ui.callback

import androidx.lifecycle.LiveData

/**
 * Created by sds100 on 28/01/2020.
 */
interface SelectionCallback {
    fun onSelect(id: Long)

    fun onUnselect(id: Long)

    fun onSelectAll()
}