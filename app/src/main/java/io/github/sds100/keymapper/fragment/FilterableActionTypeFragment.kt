package io.github.sds100.keymapper.fragment

import android.widget.Filterable
import androidx.appcompat.widget.SearchView

/**
 * Created by sds100 on 18/11/2018.
 */

abstract class FilterableActionTypeFragment : ActionTypeFragment(), SearchView.OnQueryTextListener {
    abstract val filterable: Filterable?

    override fun onQueryTextChange(newText: String?): Boolean {
        if (filterable == null) return false

        if (newText.isNullOrEmpty()) {
            filterable!!.filter.filter(null)

        } else {
            filterable!!.filter.filter(newText)
        }

        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return onQueryTextChange(query)
    }
}