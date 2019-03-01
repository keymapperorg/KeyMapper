package io.github.sds100.keymapper.delegate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.SectionItem

/**
 * Created by sds100 on 29/11/2018.
 */

class SectionedAdapterDelegate : AdapterDelegate<List<Any>>() {

    companion object {
        const val VIEW_TYPE_SECTION = 342
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.section_header_layout, parent, false)

        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(
            items: List<Any>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: MutableList<Any>
    ) {
        if (items.isSectionAtIndex(position)) {
            val sectionItem = items[position] as SectionItem
            (holder as SectionViewHolder).textView.text = sectionItem.header
        }
    }

    override fun isForViewType(items: List<Any>, position: Int): Boolean {
        return items.isSectionAtIndex(position)
    }

    private fun List<Any>.isSectionAtIndex(position: Int): Boolean {
        return this[position] is SectionItem
    }


    private class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textViewSubheading)
    }
}