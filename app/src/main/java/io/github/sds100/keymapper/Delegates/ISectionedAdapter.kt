package io.github.sds100.keymapper.Delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.SectionedItemList
import io.github.sds100.keymapper.ViewHolders.SectionViewHolder

/**
 * Created by sds100 on 29/11/2018.
 */

interface ISectionedAdapter<T> : AdapterDelegate {

    companion object {
        const val VIEW_TYPE_SECTION = 342
        const val VIEW_TYPE_DEFAULT = 837
    }

    val sectionedItemList: SectionedItemList<T>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.section_header_layout, parent, false)

        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val sectionItem = sectionedItemList.getSection(position)
        (holder as SectionViewHolder).textView.text = sectionItem.header
    }
}