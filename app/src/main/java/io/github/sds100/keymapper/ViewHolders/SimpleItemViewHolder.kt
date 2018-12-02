package io.github.sds100.keymapper.ViewHolders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.R

class SimpleItemViewHolder(
        itemView: View,
        onItemClick: (position: Int) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    val imageView: ImageView = itemView.findViewById(R.id.imageView)
    val textView: TextView = itemView.findViewById(R.id.textView)

    init {
        itemView.setOnClickListener { onItemClick(adapterPosition) }
    }
}