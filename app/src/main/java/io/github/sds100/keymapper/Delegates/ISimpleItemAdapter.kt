package io.github.sds100.keymapper.Delegates

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ViewHolders.SimpleItemViewHolder

/**
 * Created by sds100 on 29/11/2018.
 */

interface ISimpleItemAdapter<T> : AdapterDelegate{

    fun onItemClick(position: Int)
    fun getItem(position: Int): T
    fun getItemText(item: T): String
    fun getItemDrawable(item: T): Drawable?

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return SimpleItemViewHolder(inflater.inflate(R.layout.simple_recyclerview_item, parent, false))
        { position ->
            onItemClick(position)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)

        (holder as SimpleItemViewHolder).apply {
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
}