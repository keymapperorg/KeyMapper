package io.github.sds100.keymapper.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.keymap_adapter_item.view.*

/**
 * Created by sds100 on 12/07/2018.
 */

/**
 * Display a list of [KeyMap]s in a RecyclerView
 */
class KeymapAdapter(var keyMapList: List<KeyMap>
) : RecyclerView.Adapter<KeymapAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.keymap_adapter_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.apply {
            val keyMap = keyMapList[position]
            val triggerAdapter = TriggerAdapter(keyMap.triggerList, showRemoveButton = false)

            textViewTitle.text = Action.getDescription(context, keyMap.action)

            recyclerViewTriggers.layoutManager = LinearLayoutManager(context)
            recyclerViewTriggers.adapter = triggerAdapter

            /*if no icon should be shown then hide the ImageView so there isn't whitespace next to
            the text*/
            val drawable = Action.getIcon(context, keyMap.action)
            if (drawable == null) {
                imageView.setImageDrawable(null)
                imageView.visibility = View.GONE
            } else {
                imageView.setImageDrawable(drawable)
                imageView.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount() = keyMapList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}