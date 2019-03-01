package io.github.sds100.keymapper.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.selection.ISelectionProvider
import io.github.sds100.keymapper.selection.SelectionEvent

abstract class SelectableViewHolder(
        iSelectionProvider: ISelectionProvider,
        itemView: View
) : RecyclerView.ViewHolder(itemView),
        View.OnLongClickListener, View.OnClickListener, ISelectionProvider by iSelectionProvider {

    init {
        itemView.setOnLongClickListener(this)
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (inSelectingMode) {
            toggleSelection(itemId)
        }
    }

    override fun onLongClick(v: View?): Boolean {
        if (!inSelectingMode) {
            toggleSelection(itemId)
        }

        return true
    }

    /**
     * Called when an item's selection state is toggled
     */
    abstract fun onSelectionEvent(event: SelectionEvent)
}