package io.github.sds100.keymapper.adapter

import com.hannesdorfmann.adapterdelegates4.AbsDelegationAdapter
import io.github.sds100.keymapper.SimpleRecyclerViewItem
import io.github.sds100.keymapper.delegate.SimpleItemAdapterDelegate
import io.github.sds100.keymapper.interfaces.ISimpleItemAdapter
import io.github.sds100.keymapper.interfaces.OnItemClickListener

/**
 * Created by sds100 on 20/07/2019.
 */

class SimpleItemAdapter : AbsDelegationAdapter<List<SimpleRecyclerViewItem>>(),
    ISimpleItemAdapter<SimpleRecyclerViewItem> {

    override val onItemClickListener: OnItemClickListener<SimpleRecyclerViewItem>? = null

    init {
        val simpleItemDelegate = SimpleItemAdapterDelegate(this)
        delegatesManager.addDelegate(simpleItemDelegate)
    }

    override fun getItemCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemText(item: SimpleRecyclerViewItem) = item.text
    override fun getItemDrawable(item: SimpleRecyclerViewItem) = item.icon
    override fun getSecondaryItemText(item: SimpleRecyclerViewItem) = item.secondaryText
}