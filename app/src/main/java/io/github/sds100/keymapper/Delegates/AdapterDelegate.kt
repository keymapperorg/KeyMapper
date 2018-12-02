package io.github.sds100.keymapper.Delegates

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by sds100 on 30/11/2018.
 */

interface AdapterDelegate {
    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
    fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)
}