package io.github.sds100.keymapper.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Trigger
import io.github.sds100.keymapper.view.SquareImageButton
import kotlinx.android.synthetic.main.trigger_adapter_item.view.*

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * Display a list of [Trigger]s as Chips in a RecyclerView
 */
class TriggerAdapter(
        triggerList: MutableList<Trigger> = mutableListOf(),
        val showRemoveButton: Boolean = true
) : RecyclerView.Adapter<TriggerAdapter.ViewHolder>() {

    var triggerList: MutableList<Trigger> = mutableListOf()
        set(value) {
            notifyDataSetChanged()
            field = value
        }

    init {
        this.triggerList = triggerList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.trigger_adapter_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.apply {
            triggerChipGroup.addChipsFromTrigger(triggerList[position])

            if (!showRemoveButton) {
                buttonRemove.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = triggerList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.findViewById<SquareImageButton>(R.id.buttonRemove).setOnClickListener {
                if (adapterPosition in 0..itemCount) {
                    triggerList.removeAt(adapterPosition)
                    notifyItemRemoved(adapterPosition)
                }
            }
        }
    }
}