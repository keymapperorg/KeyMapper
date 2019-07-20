package io.github.sds100.keymapper.delegate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.interfaces.ISimpleItemAdapter

/**
 * Created by sds100 on 29/11/2018.
 */

class SimpleItemAdapterDelegate<T>(
    iSimpleItemAdapter: ISimpleItemAdapter<T>
) : AdapterDelegate<List<T>>(), ISimpleItemAdapter<T> by iSimpleItemAdapter {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return SimpleItemViewHolder(inflater.inflate(R.layout.simple_recyclerview_item, parent, false))
    }

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val item = getItem(position) ?: return

        (holder as SimpleItemAdapterDelegate<*>.SimpleItemViewHolder).apply {
            val drawable = getItemDrawable(item)

            /*if no icon should be shown then hide the ImageView so there isn't whitespace next to
            the text*/
            if (drawable == null) {
                imageView.setImageDrawable(null)
                imageView.visibility = View.GONE
            } else {
                imageView.setImageDrawable(drawable)
                imageView.visibility = View.VISIBLE
            }

            textView.text = getItemText(item)

            if (getSecondaryItemText(item) != null) {
                textViewSecondary.text = getSecondaryItemText(item)
            }

            if (getSecondaryItemTextColor(position) != null){
                textViewSecondary.setTextColor(getSecondaryItemTextColor(position)!!)
            }

            textViewSecondary.isVisible = getSecondaryItemText(item) != null
        }
    }

    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return getItem(position) != null
    }

    private inner class SimpleItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val textView: TextView = itemView.findViewById(R.id.textView)
        val textViewSecondary: TextView = itemView.findViewById(R.id.textViewSecondary)

        init {
            itemView.setOnClickListener {
                onItemClickListener?.onItemClick(getItem(adapterPosition)!!)
            }
        }
    }
}