package io.github.sds100.keymapper

import android.widget.Filter

/**
 * Created by sds100 on 20/11/2018.
 */

class AlphabeticalFilter<T>(private val mOriginalList: List<T>,
                            private val getItemText: (item: T) -> String,
                            private val onFilter: (filteredList: List<T>) -> Unit
) : Filter() {
    override fun performFiltering(constraint: CharSequence?): FilterResults {
        val filteredList: List<T>

        if (constraint.isNullOrEmpty()) {
            filteredList = mOriginalList
        } else {

            val query = constraint.toString().toLowerCase()
            filteredList = mutableListOf()

            mOriginalList.forEach {
                val label = getItemText(it)
                if (label.toLowerCase().contains(query)) {
                    filteredList.add(it)
                }
            }
        }

        val filterResults = FilterResults()
        filterResults.values = filteredList

        return filterResults
    }

    @Suppress("UNCHECKED_CAST")
    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
        if (results != null) onFilter(results.values as List<T>)
    }
}