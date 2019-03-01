package io.github.sds100.keymapper.adapter

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by sds100 on 21/10/2018.
 */

abstract class BaseRecyclerViewAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    /**
     * Where viewholders are cached
     */
    val boundViewHolders: MutableList<VH> = mutableListOf()

    override fun onBindViewHolder(holder: VH, position: Int) {
        boundViewHolders.add(holder)
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)

        boundViewHolders.remove(holder)
    }

    /**
     * rebind each bound view holder
     */
    fun invalidateBoundViewHolders() {
        boundViewHolders.forEach {
            notifyItemChanged(it.adapterPosition)
        }
    }
}