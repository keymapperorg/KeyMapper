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
) : RecyclerView.Adapter<VH>(), SelectionCallback {

    val iSelectionProvider: ISelectionProvider = SelectionProvider(
            allItemIds = itemList.map { it.id }
    )

    var itemList: List<T> = listOf()
        set(value) {
            iSelectionProvider.allItemIds = value.map { it.id }
            field = value
        }

    private var mRecyclerView: RecyclerView? = null

    init {
        iSelectionProvider.subscribeToSelectionEvents(this)

        //improves performance by allowing the adapter to find viewholders by their ids
        this.setHasStableIds(true)

        this.itemList = itemList
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        mRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        mRecyclerView = null
    }

    override fun onSelectionEvent(id: Long?, event: SelectionEvent) {
        //if the event affects only a single viewholder.
        if (id != null) {
            if (mRecyclerView != null) {
                val viewHolder = mRecyclerView!!.findViewHolderForItemId(id)

                if (viewHolder != null) {
                    @Suppress("UNCHECKED_CAST")
                    (viewHolder as VH).onSelectionEvent(event)
                }
            }

        //if the event affects all viewholders
        } else {
            iSelectionProvider.allItemIds.forEach {
                if (mRecyclerView != null) {
                    val viewHolder = mRecyclerView!!.findViewHolderForItemId(it)

                    if (viewHolder != null) {
                        @Suppress("UNCHECKED_CAST")
                        (viewHolder as VH).onSelectionEvent(event)
                    }
                }
            }
        }
    }

    override fun getItemCount() = itemList.size

    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnLongClickListener {
                if (!iSelectionProvider.inSelectingMode) {
                    iSelectionProvider.toggleSelection(getItemId(adapterPosition))
                }

                true
            }

            itemView.setOnClickListener {
                if (iSelectionProvider.inSelectingMode) {
                    iSelectionProvider.toggleSelection(getItemId(adapterPosition))
                }
            }
        }

        /**
         * Called when an item's selection state is toggled
         */
        abstract fun onSelectionEvent(event: SelectionEvent)
    }
}