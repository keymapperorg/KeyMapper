package io.github.sds100.keymapper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by sds100 on 16/07/2018.
 */

class TriggerAdapter(val triggerList: MutableList<Trigger> = mutableListOf()
) : RecyclerView.Adapter<TriggerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.trigger_adapter_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            chipGroupTrigger.addChipsFromTrigger(triggerList[position])
        }
    }

    override fun getItemCount() = triggerList.size

    /**
     * Add a new trigger to the list
     */
    fun addTrigger(trigger: Trigger) {
        triggerList.add(trigger)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chipGroupTrigger: TriggerChipGroup = itemView.findViewById(R.id.chipGroupTrigger)

        init {
            itemView.findViewById<SquareImageButton>(R.id.buttonRemove).setOnClickListener {
                triggerList.removeAt(adapterPosition)
                notifyItemRemoved(adapterPosition)
            }
        }
    }
}