package io.github.sds100.keymapper.Delegates

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import io.github.sds100.keymapper.Interfaces.ISimpleItemAdapter
import io.github.sds100.keymapper.R

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
        }
    }

    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return getItem(position) != null
    }

    private inner class SimpleItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val textView: TextView = itemView.findViewById(R.id.textView)

        init {
            itemView.setOnClickListener { onItemClickListener.onItemClick(getItem(adapterPosition)!!) }
        }
    }
}