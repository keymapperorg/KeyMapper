package io.github.sds100.keymapper.Adapters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 17/07/2018.
 */

/**
 * Display an icon and text in a RecyclerView
 */
abstract class SimpleItemAdapter<T>(
        private val itemList: List<T>,
        private val onItemClickListener: OnItemClickListener<T>
) : RecyclerView.Adapter<SimpleItemAdapter<T>.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.simple_recyclerview_item, parent, false))
    }

    override fun getItemCount() = itemList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]

        holder.apply {
            imageView.setImageDrawable(getItemImage(item))
            textView.text = getItemText(item)
        }
    }

    abstract fun getItemText(item: T): String
    abstract fun getItemImage(item: T): Drawable?

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val textView: TextView = itemView.findViewById(R.id.textView)

        init {
            itemView.setOnClickListener {
                onItemClickListener.onItemClick(itemList[adapterPosition])
            }
        }
    }

    interface OnItemClickListener<T> {
        fun onItemClick(item: T)
    }
}