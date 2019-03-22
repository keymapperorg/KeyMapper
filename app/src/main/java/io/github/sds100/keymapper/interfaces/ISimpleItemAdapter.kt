package io.github.sds100.keymapper.interfaces

import android.graphics.drawable.Drawable

/**
 * Created by sds100 on 03/12/2018.
 */

interface ISimpleItemAdapter<T> {
    val onItemClickListener: OnItemClickListener<T>
    fun getItem(position: Int): T?
    fun getItemText(item: T): String
    fun getItemDrawable(item: T): Drawable?
}