package io.github.sds100.keymapper.ViewHolders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 01/12/2018.
 */


class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val textView: TextView = itemView.findViewById(R.id.textViewSubheading)
}