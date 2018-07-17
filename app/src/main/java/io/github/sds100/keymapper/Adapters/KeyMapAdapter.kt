package io.github.sds100.keymapper.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 12/07/2018.
 */
class KeyMapAdapter(private val keyMapList: List<KeyMap>
) : RecyclerView.Adapter<KeyMapAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.keymap_adapter_item, null))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.itemView.apply {

        }
    }

    override fun getItemCount() = keyMapList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }
}