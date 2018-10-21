package io.github.sds100.keymapper.Adapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.Selection.*

/**
 * Created by sds100 on 12/08/2018.
 */

/**
 * A RecyclerView Adapter which allows items to be selected when long pressed
 * @param T The object type for the items
 */
abstract class SelectableAdapter<T : SelectableItem, VH : SelectableAdapter<T, VH>.ViewHolder>(
        itemList: List<T> = listOf()
) : BaseRecyclerViewAdapter<VH>(), SelectionCallback {

    val iSelectionProvider: ISelectionProvider = SelectionProvider(
            allItemIds = itemList.map { it.id }
    )

    var itemList: List<T> = listOf()
        set(value) {
            iSelectionProvider.allItemIds = value.map { it.id }
            field = value
        }

    init {
        iSelectionProvider.subscribeToSelectionEvents(this)

        //improves performance by allowing the adapter to find viewholders by their ids
        this.setHasStableIds(true)

        this.itemList = itemList
    }

    override fun onSelectionEvent(id: Long?, event: SelectionEvent) {
        //if the event affects only a single viewholder.
        if (id != null) {
            boundViewHolders.find { it.itemId == id }!!.onSelectionEvent(event)
        } else {
            boundViewHolders.forEach { viewHolder ->
                viewHolder.onSelectionEvent(event)
            }
        }
    }

    override fun getItemCount() = itemList.size

    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnLongClickListener, View.OnClickListener {

        init {
            itemView.setOnLongClickListener(this)
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            if (iSelectionProvider.inSelectingMode) {
                iSelectionProvider.toggleSelection(getItemId(adapterPosition))
            }
        }

        override fun onLongClick(v: View?): Boolean {
            if (!iSelectionProvider.inSelectingMode) {
                iSelectionProvider.toggleSelection(getItemId(adapterPosition))
            }

            return true
        }

        /**
         * Called when an item's selection state is toggled
         */
        abstract fun onSelectionEvent(event: SelectionEvent)
    }
}